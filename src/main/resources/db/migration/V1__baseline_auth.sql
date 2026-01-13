-- ============================================================================
-- V1__baseline_auth_and_parties.sql  (PostgreSQL)
-- Scalaris: Auth (users + refresh + password reset) + Parties (clientes/proveedores)
--
-- Flyway: no BEGIN/COMMIT (Flyway maneja transacción).
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- Common: updated_at auto-maintenance (opcional, pero práctico)
-- ============================================================================
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
RETURN NEW;
END;
$$;

-- ============================================================================
-- AUTH: app_user
-- ============================================================================
CREATE TABLE app_user (
                          id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                          full_name         varchar(40)  NOT NULL,
                          email             varchar(254) NOT NULL,
                          password_hash     varchar(120) NOT NULL,

                          role              varchar(20)  NOT NULL DEFAULT 'EMPLOYEE',
                          tax_position      varchar(30),
                          company_structure varchar(30),

                          active            boolean      NOT NULL DEFAULT true,
                          accepted_terms    boolean      NOT NULL,

                          created_at        timestamptz  NOT NULL DEFAULT now(),
                          updated_at        timestamptz  NOT NULL DEFAULT now()
);

ALTER TABLE app_user
    ADD CONSTRAINT uk_app_user_email UNIQUE (email);

CREATE TRIGGER trg_app_user_set_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- AUTH: refresh_token (revocación de refresh JWT)
-- ============================================================================
CREATE TABLE refresh_token (
                               id          uuid PRIMARY KEY,               -- jti del refresh JWT
                               user_id     uuid        NOT NULL,
                               expires_at  timestamptz NOT NULL,
                               revoked_at  timestamptz NULL,
                               created_at  timestamptz NOT NULL DEFAULT now(),

                               CONSTRAINT fk_refresh_user
                                   FOREIGN KEY (user_id) REFERENCES app_user(id)
                                       ON DELETE CASCADE
);

CREATE INDEX ix_refresh_user    ON refresh_token(user_id);
CREATE INDEX ix_refresh_expires ON refresh_token(expires_at);

-- ============================================================================
-- AUTH: password_reset_token (CUS-04)
-- ============================================================================
CREATE TABLE password_reset_token (
                                      id          uuid PRIMARY KEY,
                                      user_id     uuid        NOT NULL,
                                      code_hash   varchar(120) NOT NULL,
                                      expires_at  timestamptz NOT NULL,
                                      used_at     timestamptz NULL,
                                      created_at  timestamptz NOT NULL DEFAULT now(),

                                      CONSTRAINT fk_prt_user
                                          FOREIGN KEY (user_id) REFERENCES app_user(id)
                                              ON DELETE CASCADE
);

CREATE INDEX ix_prt_user    ON password_reset_token(user_id);
CREATE INDEX ix_prt_expires ON password_reset_token(expires_at);

-- ============================================================================
-- PARTIES: third_party (clientes / proveedores)
-- Incluye QA del cliente (datos extra) sin volverse un monstruo inmantenible.
-- ============================================================================
CREATE TABLE third_party (
                             id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),

                             kind              varchar(15)  NOT NULL,   -- CUSTOMER | SUPPLIER | BOTH
                             person_type       varchar(10)  NOT NULL,   -- PERSON | COMPANY

                             display_name      varchar(120) NOT NULL,   -- "Nombre y apellido / Razón social"
                             legal_name        varchar(160) NULL,

                             email             varchar(254) NULL,
                             phone             varchar(40)  NULL,

                             document_type     varchar(20)  NULL,       -- DNI, etc
                             document_number   varchar(40)  NULL,       -- "número único" (si aplica)

                             birth_date        date         NULL,
                             marital_status    varchar(15)  NULL,       -- SINGLE|MARRIED|...
                             children_count    integer      NULL,
                             houses_count      integer      NULL,
                             has_partner       boolean      NULL,

                             company_name      varchar(160) NULL,
                             office_name       varchar(160) NULL,
                             employees_count   integer      NULL,
                             style_preference  varchar(15)  NULL,       -- RUSTIC|MODERN|...

                             tax_position      varchar(30)  NULL,       -- CONSUMIDOR_FINAL|...
                             company_structure varchar(30)  NULL,       -- UNIPERSONAL|SRL|...

                             notes             text         NULL,

                             active            boolean      NOT NULL DEFAULT true,

                             created_at        timestamptz  NOT NULL DEFAULT now(),
                             updated_at        timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX ix_tp_kind   ON third_party(kind);
CREATE INDEX ix_tp_active ON third_party(active);

-- Email único si existe (evita duplicados sin obligar a cargarlo)
CREATE UNIQUE INDEX uk_tp_email_not_null
    ON third_party(email)
    WHERE email IS NOT NULL;

-- Documento único si existe (document_type + document_number)
CREATE UNIQUE INDEX uk_tp_document_not_null
    ON third_party(document_type, document_number)
    WHERE document_type IS NOT NULL AND document_number IS NOT NULL;

CREATE TRIGGER trg_third_party_set_updated_at
    BEFORE UPDATE ON third_party
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- PARTIES: third_party_address (múltiples domicilios, incluyendo fiscal)
-- ============================================================================
CREATE TABLE third_party_address (
                                     id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                     third_party_id uuid        NOT NULL,

                                     address_type  varchar(15)  NOT NULL,    -- HOME|DELIVERY|BUSINESS|FISCAL|...
                                     line1         varchar(200) NOT NULL,
                                     line2         varchar(200) NULL,
                                     city          varchar(80)  NULL,
                                     state         varchar(80)  NULL,
                                     zip           varchar(20)  NULL,
                                     country       varchar(80)  NULL,

                                     is_primary    boolean      NOT NULL DEFAULT false,
                                     created_at    timestamptz  NOT NULL DEFAULT now(),

                                     CONSTRAINT fk_tpa_party
                                         FOREIGN KEY (third_party_id) REFERENCES third_party(id)
                                             ON DELETE CASCADE
);

CREATE INDEX ix_tpa_party ON third_party_address(third_party_id);

-- 1 domicilio "primary" por ficha (si querés uno por tipo, se ajusta después)
CREATE UNIQUE INDEX uk_tpa_primary_per_party
    ON third_party_address(third_party_id)
    WHERE is_primary = true;

-- ============================================================================
-- PARTIES: third_party_tax_id (múltiples CUIT/CUIL/DNI, etc.)
-- ============================================================================
CREATE TABLE third_party_tax_id (
                                    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                    third_party_id uuid        NOT NULL,

                                    tax_id_type   varchar(15)  NOT NULL,    -- CUIT|CUIL|DNI|OTHER
                                    value         varchar(40)  NOT NULL,

                                    is_primary    boolean      NOT NULL DEFAULT false,
                                    created_at    timestamptz  NOT NULL DEFAULT now(),

                                    CONSTRAINT fk_tpt_party
                                        FOREIGN KEY (third_party_id) REFERENCES third_party(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT uk_tpt_type_value UNIQUE (tax_id_type, value)
);

CREATE INDEX ix_tpt_party ON third_party_tax_id(third_party_id);

-- 1 tax id "primary" por ficha
CREATE UNIQUE INDEX uk_tpt_primary_per_party
    ON third_party_tax_id(third_party_id)
    WHERE is_primary = true;
