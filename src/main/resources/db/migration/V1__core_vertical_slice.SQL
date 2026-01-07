-- ============================================================================
-- V1__baseline_init.sql  (PostgreSQL 17)
-- Scalaris - Baseline (Platform + Identity)
-- Flyway: sin BEGIN/COMMIT (Flyway maneja la transacción).
-- ============================================================================

-- UUID generator + crypt()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Defensive updated_at auto-maintenance
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- HARDENING: multi-tenant consistency checks (join/child tables)
-- ---------------------------------------------------------------------------

-- user_role: ensure app_user.tenant_id == role.tenant_id
CREATE OR REPLACE FUNCTION trg_user_role_assert_same_tenant()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
v_user_tenant uuid;
  v_role_tenant uuid;
BEGIN
SELECT tenant_id INTO v_user_tenant FROM app_user WHERE id = NEW.user_id;
SELECT tenant_id INTO v_role_tenant FROM role     WHERE id = NEW.role_id;

IF v_user_tenant IS NULL THEN
    RAISE EXCEPTION 'user_role: user_id % not found', NEW.user_id;
END IF;
  IF v_role_tenant IS NULL THEN
    RAISE EXCEPTION 'user_role: role_id % not found', NEW.role_id;
END IF;

  IF v_user_tenant <> v_role_tenant THEN
    RAISE EXCEPTION 'user_role: cross-tenant assignment user % (tenant %) vs role % (tenant %)',
      NEW.user_id, v_user_tenant, NEW.role_id, v_role_tenant;
END IF;

RETURN NEW;
END;
$$;

-- user_session: ensure user_session.tenant_id == app_user.tenant_id AND stamp matches at insert
CREATE OR REPLACE FUNCTION trg_user_session_assert_user_tenant_and_stamp()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
v_user_tenant uuid;
  v_user_stamp  uuid;
BEGIN
SELECT tenant_id, security_stamp
INTO v_user_tenant, v_user_stamp
FROM app_user
WHERE id = NEW.user_id;

IF v_user_tenant IS NULL THEN
    RAISE EXCEPTION 'user_session: user_id % not found', NEW.user_id;
END IF;

  IF NEW.tenant_id <> v_user_tenant THEN
    RAISE EXCEPTION 'user_session: cross-tenant session user % (tenant %) vs session tenant %',
      NEW.user_id, v_user_tenant, NEW.tenant_id;
END IF;

  IF NEW.security_stamp IS NULL THEN
    RAISE EXCEPTION 'user_session: security_stamp required';
END IF;

  IF NEW.security_stamp <> v_user_stamp THEN
    RAISE EXCEPTION 'user_session: security_stamp mismatch for user % (expected %, got %)',
      NEW.user_id, v_user_stamp, NEW.security_stamp;
END IF;

RETURN NEW;
END;
$$;

-- password_reset_token: ensure password_reset_token.tenant_id == app_user.tenant_id
CREATE OR REPLACE FUNCTION trg_prt_assert_user_tenant()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
v_user_tenant uuid;
BEGIN
SELECT tenant_id INTO v_user_tenant
FROM app_user
WHERE id = NEW.user_id;

IF v_user_tenant IS NULL THEN
    RAISE EXCEPTION 'password_reset_token: user_id % not found', NEW.user_id;
END IF;

  IF NEW.tenant_id <> v_user_tenant THEN
    RAISE EXCEPTION 'password_reset_token: cross-tenant token user % (tenant %) vs token tenant %',
      NEW.user_id, v_user_tenant, NEW.tenant_id;
END IF;

RETURN NEW;
END;
$$;

-- ============================================================================
-- PLATFORM: tenant (AuditedEntity: no tenant_id)
-- ============================================================================
CREATE TABLE tenant (
                        id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                        created_at    timestamptz NOT NULL DEFAULT now(),
                        updated_at    timestamptz NOT NULL DEFAULT now(),
                        created_by    uuid,
                        updated_by    uuid,
                        active        boolean NOT NULL DEFAULT true,
                        version       bigint  NOT NULL DEFAULT 0,

                        name          varchar(120) NOT NULL,
                        status        varchar(30)  NOT NULL DEFAULT 'ACTIVE',
                        plan          varchar(30)  NOT NULL DEFAULT 'BASIC',
                        settings_json jsonb        NOT NULL DEFAULT '{}'::jsonb,

                        CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE','SUSPENDED','CANCELED')),
                        CONSTRAINT chk_tenant_plan   CHECK (plan   IN ('BASIC','PRO','ENTERPRISE'))
);

CREATE INDEX ix_tenant_status ON tenant (status);

CREATE TRIGGER set_updated_at_tenant
    BEFORE UPDATE ON tenant
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- PLATFORM: tenant_key (AuditedEntity)
-- ============================================================================
CREATE TABLE tenant_key (
                            id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                            created_at timestamptz NOT NULL DEFAULT now(),
                            updated_at timestamptz NOT NULL DEFAULT now(),
                            created_by uuid,
                            updated_by uuid,
                            active     boolean NOT NULL DEFAULT true,
                            version    bigint  NOT NULL DEFAULT 0,

                            tenant_id  uuid NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
                            key_type   varchar(30)  NOT NULL DEFAULT 'SLUG',
                            key_value  varchar(120) NOT NULL,

                            CONSTRAINT uk_tenant_key_type_value UNIQUE (key_type, key_value),
                            CONSTRAINT uk_tenant_key_tenant_type UNIQUE (tenant_id, key_type)
);

-- Case-insensitive global uniqueness for (key_type, key_value)
CREATE UNIQUE INDEX ux_tenant_key_type_value_ci
    ON tenant_key (key_type, lower(key_value));

CREATE INDEX ix_tenant_key_tenant
    ON tenant_key (tenant_id);

CREATE TRIGGER set_updated_at_tenant_key
    BEFORE UPDATE ON tenant_key
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- PLATFORM: tenant_entitlement (AuditedTenantEntity)
-- ============================================================================
CREATE TABLE tenant_entitlement (
                                    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                                    tenant_id  uuid NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,

                                    created_at timestamptz NOT NULL DEFAULT now(),
                                    updated_at timestamptz NOT NULL DEFAULT now(),
                                    created_by uuid,
                                    updated_by uuid,
                                    active     boolean NOT NULL DEFAULT true,
                                    version    bigint  NOT NULL DEFAULT 0,

                                    module_code varchar(60) NOT NULL,
                                    enabled     boolean     NOT NULL DEFAULT false,
                                    limits_json jsonb       NOT NULL DEFAULT '{}'::jsonb,

                                    CONSTRAINT uk_tenant_ent_module UNIQUE (tenant_id, module_code)
);

CREATE INDEX ix_tenant_ent_tenant
    ON tenant_entitlement (tenant_id);

CREATE TRIGGER set_updated_at_tenant_entitlement
    BEFORE UPDATE ON tenant_entitlement
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- SHARED: tenant_sequence (AuditedTenantEntity)
-- ============================================================================
CREATE TABLE tenant_sequence (
                                 id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                                 tenant_id  uuid NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,

                                 created_at timestamptz NOT NULL DEFAULT now(),
                                 updated_at timestamptz NOT NULL DEFAULT now(),
                                 created_by uuid,
                                 updated_by uuid,
                                 active     boolean NOT NULL DEFAULT true,
                                 version    bigint  NOT NULL DEFAULT 0,

                                 seq_key    varchar(40) NOT NULL,
                                 next_value bigint      NOT NULL,

                                 CONSTRAINT uk_seq_tenant_key UNIQUE (tenant_id, seq_key)
);

CREATE INDEX ix_tenant_seq_tenant_key
    ON tenant_sequence (tenant_id, seq_key);

CREATE TRIGGER set_updated_at_tenant_sequence
    BEFORE UPDATE ON tenant_sequence
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- IDENTITY: role (AuditedTenantEntity)
-- ============================================================================
CREATE TABLE role (
                      id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                      tenant_id  uuid NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,

                      created_at timestamptz NOT NULL DEFAULT now(),
                      updated_at timestamptz NOT NULL DEFAULT now(),
                      created_by uuid,
                      updated_by uuid,
                      active     boolean NOT NULL DEFAULT true,
                      version    bigint  NOT NULL DEFAULT 0,

                      name       varchar(80) NOT NULL,

                      CONSTRAINT uk_role_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX ix_role_tenant
    ON role (tenant_id);

CREATE TRIGGER set_updated_at_role
    BEFORE UPDATE ON role
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ElementCollection: Role.permissions
CREATE TABLE role_permission (
                                 role_id          uuid NOT NULL REFERENCES role(id) ON DELETE CASCADE,
                                 permission_code  varchar(80) NOT NULL,
                                 CONSTRAINT uk_role_perm UNIQUE (role_id, permission_code)
);

CREATE INDEX ix_role_perm_role
    ON role_permission(role_id);

-- ============================================================================
-- IDENTITY: app_user (AuditedTenantEntity)
-- (incluye lockout fields)
-- ============================================================================
CREATE TABLE app_user (
                          id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                          tenant_id     uuid NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,

                          created_at    timestamptz NOT NULL DEFAULT now(),
                          updated_at    timestamptz NOT NULL DEFAULT now(),
                          created_by    uuid,
                          updated_by    uuid,
                          active        boolean NOT NULL DEFAULT true,
                          version       bigint  NOT NULL DEFAULT 0,

                          email         varchar(180) NOT NULL,
                          password_hash varchar(200) NULL,
                          full_name     varchar(120) NOT NULL,
                          phone         varchar(40),

                          status        varchar(30)  NOT NULL DEFAULT 'PENDING',
                          last_login_at timestamptz,

                          security_stamp uuid NOT NULL DEFAULT gen_random_uuid(),

                          failed_login_count   integer     NOT NULL DEFAULT 0,
                          locked_until         timestamptz,
                          last_failed_login_at timestamptz,

                          CONSTRAINT chk_user_status CHECK (status IN ('PENDING','ACTIVE','INACTIVE','LOCKED')),
                          CONSTRAINT uk_user_tenant_email UNIQUE (tenant_id, email)
);

-- Case-insensitive unique per tenant
CREATE UNIQUE INDEX ux_app_user_tenant_email_ci
    ON app_user (tenant_id, lower(email));

CREATE INDEX ix_app_user_tenant
    ON app_user (tenant_id);

CREATE TRIGGER set_updated_at_app_user
    BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- IDENTITY: user_role (join)
-- ============================================================================
CREATE TABLE user_role (
                           user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                           role_id uuid NOT NULL REFERENCES role(id)     ON DELETE CASCADE,
                           CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX ix_user_role_user ON user_role(user_id);
CREATE INDEX ix_user_role_role ON user_role(role_id);

-- Enforce same-tenant user<->role
CREATE TRIGGER trg_user_role_same_tenant
    BEFORE INSERT OR UPDATE ON user_role
                         FOR EACH ROW EXECUTE FUNCTION trg_user_role_assert_same_tenant();

-- ============================================================================
-- IDENTITY: user_session (AuditedTenantEntity)
-- ============================================================================
CREATE TABLE user_session (
                              id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                              tenant_id  uuid NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,

                              created_at timestamptz NOT NULL DEFAULT now(),
                              updated_at timestamptz NOT NULL DEFAULT now(),
                              created_by uuid,
                              updated_by uuid,
                              active     boolean NOT NULL DEFAULT true,
                              version    bigint  NOT NULL DEFAULT 0,

                              user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

                              session_token_hash varchar(200) NOT NULL,
                              ip                 varchar(80),
                              user_agent         varchar(400),

                              expires_at   timestamptz NOT NULL,
                              last_seen_at timestamptz NOT NULL,
                              revoked_at   timestamptz,
                              revoked_by   uuid,

                              security_stamp uuid NOT NULL,

                              CONSTRAINT uk_session_token_hash UNIQUE (session_token_hash)
);

CREATE INDEX ix_session_user     ON user_session(user_id);
CREATE INDEX ix_session_expires  ON user_session(expires_at);

CREATE INDEX ix_session_tenant_user_rev
    ON user_session (tenant_id, user_id, revoked_at);

CREATE INDEX ix_session_tenant_expires_rev
    ON user_session (tenant_id, expires_at, revoked_at);

CREATE TRIGGER set_updated_at_user_session
    BEFORE UPDATE ON user_session
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- Enforce tenant match + stamp match
CREATE TRIGGER trg_user_session_tenant_stamp
    BEFORE INSERT OR UPDATE ON user_session
                         FOR EACH ROW EXECUTE FUNCTION trg_user_session_assert_user_tenant_and_stamp();

-- ============================================================================
-- IDENTITY: password_reset_token (AuditedTenantEntity)
-- ============================================================================
CREATE TABLE password_reset_token (
                                      id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                                      tenant_id  uuid NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,

                                      created_at timestamptz NOT NULL DEFAULT now(),
                                      updated_at timestamptz NOT NULL DEFAULT now(),
                                      created_by uuid,
                                      updated_by uuid,
                                      active     boolean NOT NULL DEFAULT true,
                                      version    bigint  NOT NULL DEFAULT 0,

                                      user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

                                      token_hash varchar(200) NOT NULL,
                                      expires_at timestamptz NOT NULL,
                                      used_at    timestamptz,

                                      purpose    varchar(20)  NOT NULL DEFAULT 'RESET',

                                      CONSTRAINT chk_prt_purpose CHECK (purpose IN ('RESET','INVITE')),
                                      CONSTRAINT uk_prt_tenant_token_hash UNIQUE (tenant_id, token_hash)
);

-- Indexes as per entity + operational
CREATE INDEX ix_prt_user
    ON password_reset_token (user_id);

CREATE INDEX ix_prt_expires
    ON password_reset_token (expires_at);

CREATE INDEX ix_prt_tenant_hash_purpose_expires
    ON password_reset_token (tenant_id, token_hash, purpose, expires_at);

CREATE INDEX ix_prt_tenant_hash_purpose_used
    ON password_reset_token (tenant_id, token_hash, purpose, used_at);

CREATE TRIGGER set_updated_at_password_reset_token
    BEFORE UPDATE ON password_reset_token
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- Enforce tenant match
CREATE TRIGGER trg_prt_same_tenant
    BEFORE INSERT OR UPDATE ON password_reset_token
                         FOR EACH ROW EXECUTE FUNCTION trg_prt_assert_user_tenant();

-- ============================================================================
-- Spring Modulith (si lo estás usando)
-- ============================================================================
CREATE TABLE event_publication (
                                   id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                   listener_id       varchar(255) NOT NULL,
                                   event_type        varchar(255) NOT NULL,
                                   serialized_event  text NOT NULL,
                                   publication_date  timestamptz NOT NULL DEFAULT now(),
                                   completion_date   timestamptz,
                                   tenant_id         uuid NULL
);

CREATE INDEX ix_event_publication_completion_date
    ON event_publication (completion_date);

CREATE INDEX ix_event_publication_listener
    ON event_publication (listener_id);
