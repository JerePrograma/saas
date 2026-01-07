-- ============================================================================
-- R__seed.sql (idempotente) - Scalaris (PG17)
-- Seeds: Platform tenant + Demo tenant
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
  -- =========================
  -- PLATFORM TENANT (debe coincidir con PlatformConstants.PLATFORM_TENANT_ID)
  -- =========================
v_platform_tenant_id uuid := '00000000-0000-0000-0000-000000000001';
  v_platform_role_admin uuid := 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1';
  v_platform_user_admin uuid := 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1';
  v_platform_password_plain text := 'ChangeMe-Platform-123!';
  v_platform_password_hash  text;

  -- =========================
  -- DEMO TENANT
  -- =========================
  v_demo_tenant_id uuid := '11111111-1111-1111-1111-111111111111';

  v_demo_role_owner uuid := '22222222-2222-2222-2222-222222222221';
  v_demo_role_admin uuid := '22222222-2222-2222-2222-222222222222';
  v_demo_role_user  uuid := '22222222-2222-2222-2222-222222222223';

  v_demo_user_owner uuid := '33333333-3333-3333-3333-333333333331';
  v_demo_password_plain text := 'ChangeMe-Now-123!';
  v_demo_password_hash  text;

  -- reutilizable para upserts “por email”
  v_existing_id uuid;
BEGIN
  v_platform_password_hash := crypt(v_platform_password_plain, gen_salt('bf', 12));
  v_demo_password_hash := crypt(v_demo_password_plain, gen_salt('bf', 12));

  -- --------------------------------------------------------------------------
  -- PLATFORM: TENANT
  -- --------------------------------------------------------------------------
INSERT INTO tenant (id, name, status, plan, settings_json, active)
VALUES (v_platform_tenant_id, 'Platform', 'ACTIVE', 'ENTERPRISE', '{}'::jsonb, true)
    ON CONFLICT (id) DO UPDATE
                            SET name = EXCLUDED.name,
                            status = EXCLUDED.status,
                            plan = EXCLUDED.plan,
                            settings_json = EXCLUDED.settings_json,
                            active = true;

-- PLATFORM: TENANT_KEY (slug = platform)
INSERT INTO tenant_key (tenant_id, key_type, key_value, active)
VALUES (v_platform_tenant_id, 'SLUG', 'platform', true)
    ON CONFLICT ON CONSTRAINT uk_tenant_key_tenant_type
    DO UPDATE
           SET key_value = EXCLUDED.key_value,
           active = true;

-- PLATFORM: ENTITLEMENTS mínimos
INSERT INTO tenant_entitlement (tenant_id, module_code, enabled, limits_json)
VALUES
    (v_platform_tenant_id, 'IDENTITY', true, '{}'::jsonb)
    ON CONFLICT ON CONSTRAINT uk_tenant_ent_module
    DO UPDATE
           SET enabled = EXCLUDED.enabled,
           limits_json = EXCLUDED.limits_json,
           active = true;

-- --------------------------------------------------------------------------
-- PLATFORM: ROLE(S)
-- --------------------------------------------------------------------------
WITH up AS (
INSERT INTO role (id, tenant_id, name, active)
VALUES (v_platform_role_admin, v_platform_tenant_id, 'PLATFORM_ADMIN', true)
ON CONFLICT ON CONSTRAINT uk_role_tenant_name
    DO UPDATE SET active = true
           RETURNING id
           )
SELECT id INTO v_platform_role_admin FROM up;

-- PLATFORM: ROLE PERMISSIONS (exactos para @PreAuthorize)
INSERT INTO role_permission (role_id, permission_code)
VALUES
    (v_platform_role_admin, 'PLATFORM.TENANT.MANAGE')
    ON CONFLICT ON CONSTRAINT uk_role_perm DO NOTHING;

-- --------------------------------------------------------------------------
-- PLATFORM: USER (admin) - upsert por email CI
-- --------------------------------------------------------------------------
SELECT id
INTO v_existing_id
FROM app_user
WHERE tenant_id = v_platform_tenant_id
  AND lower(email) = lower('platform@local')
    LIMIT 1;

IF v_existing_id IS NULL THEN
    INSERT INTO app_user (id, tenant_id, email, password_hash, full_name, status, active)
    VALUES (
      v_platform_user_admin,
      v_platform_tenant_id,
      lower('platform@local'),
      v_platform_password_hash,
      'Platform Admin',
      'ACTIVE',
      true
    );
ELSE
    v_platform_user_admin := v_existing_id;

UPDATE app_user
SET email = lower('platform@local'),
    full_name = 'Platform Admin',
    status = 'ACTIVE',
    password_hash = v_platform_password_hash,
    active = true
WHERE id = v_platform_user_admin;
END IF;

INSERT INTO user_role (user_id, role_id)
VALUES (v_platform_user_admin, v_platform_role_admin)
    ON CONFLICT ON CONSTRAINT uk_user_role DO NOTHING;

-- --------------------------------------------------------------------------
-- DEMO: TENANT
-- --------------------------------------------------------------------------
INSERT INTO tenant (id, name, status, plan, settings_json, active)
VALUES (v_demo_tenant_id, 'Demo Tenant', 'ACTIVE', 'BASIC', '{}'::jsonb, true)
    ON CONFLICT (id) DO UPDATE
                            SET name = EXCLUDED.name,
                            status = EXCLUDED.status,
                            plan = EXCLUDED.plan,
                            settings_json = EXCLUDED.settings_json,
                            active = true;

-- DEMO: TENANT_KEY (slug = demo)
INSERT INTO tenant_key (tenant_id, key_type, key_value, active)
VALUES (v_demo_tenant_id, 'SLUG', 'demo', true)
    ON CONFLICT ON CONSTRAINT uk_tenant_key_tenant_type
    DO UPDATE
           SET key_value = EXCLUDED.key_value,
           active = true;

-- DEMO: ENTITLEMENTS (ajustá module_code a los reales)
INSERT INTO tenant_entitlement (tenant_id, module_code, enabled, limits_json)
VALUES
    (v_demo_tenant_id, 'CRM',           true, '{}'::jsonb),
    (v_demo_tenant_id, 'CATALOG',       true, '{}'::jsonb),
    (v_demo_tenant_id, 'QUOTATIONS',    true, '{}'::jsonb),
    (v_demo_tenant_id, 'SALES',         true, '{}'::jsonb),
    (v_demo_tenant_id, 'WORK',          true, '{}'::jsonb),
    (v_demo_tenant_id, 'INVENTORY',     true, '{}'::jsonb),
    (v_demo_tenant_id, 'FINANCE',       true, '{}'::jsonb),
    (v_demo_tenant_id, 'NOTIFICATIONS', true, '{}'::jsonb),
    (v_demo_tenant_id, 'IDENTITY',      true, '{}'::jsonb)
    ON CONFLICT ON CONSTRAINT uk_tenant_ent_module
    DO UPDATE
           SET enabled = EXCLUDED.enabled,
           limits_json = EXCLUDED.limits_json,
           active = true;

-- DEMO: TENANT SEQUENCES (ajustá seq_key a tu enum real)
INSERT INTO tenant_sequence (tenant_id, seq_key, next_value)
VALUES
    (v_demo_tenant_id, 'QUOTE_NUMBER', 1),
    (v_demo_tenant_id, 'SALE_NUMBER',  1),
    (v_demo_tenant_id, 'WORK_NUMBER',  1)
    ON CONFLICT ON CONSTRAINT uk_seq_tenant_key
    DO UPDATE
           SET next_value = GREATEST(tenant_sequence.next_value, EXCLUDED.next_value),
           active = true;

-- --------------------------------------------------------------------------
-- DEMO: ROLES
-- --------------------------------------------------------------------------
WITH up AS (
INSERT INTO role (id, tenant_id, name, active)
VALUES (v_demo_role_owner, v_demo_tenant_id, 'OWNER', true)
ON CONFLICT ON CONSTRAINT uk_role_tenant_name
    DO UPDATE SET active = true
           RETURNING id
           )
SELECT id INTO v_demo_role_owner FROM up;

WITH up AS (
INSERT INTO role (id, tenant_id, name, active)
VALUES (v_demo_role_admin, v_demo_tenant_id, 'ADMIN', true)
ON CONFLICT ON CONSTRAINT uk_role_tenant_name
    DO UPDATE SET active = true
           RETURNING id
           )
SELECT id INTO v_demo_role_admin FROM up;

WITH up AS (
INSERT INTO role (id, tenant_id, name, active)
VALUES (v_demo_role_user, v_demo_tenant_id, 'USER', true)
ON CONFLICT ON CONSTRAINT uk_role_tenant_name
    DO UPDATE SET active = true
           RETURNING id
           )
SELECT id INTO v_demo_role_user FROM up;

-- DEMO: ROLE PERMISSIONS (wildcards / ejemplos)
INSERT INTO role_permission (role_id, permission_code)
VALUES
    (v_demo_role_owner, 'platform:*'),
    (v_demo_role_owner, 'tenant:*'),
    (v_demo_role_owner, 'identity:*'),
    (v_demo_role_owner, 'crm:*'),
    (v_demo_role_owner, 'sales:*'),
    (v_demo_role_owner, 'work:*'),
    (v_demo_role_owner, 'inventory:*'),
    (v_demo_role_owner, 'finance:*'),
    (v_demo_role_owner, 'notifications:*')
    ON CONFLICT ON CONSTRAINT uk_role_perm DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
VALUES
    (v_demo_role_admin, 'tenant:*'),
    (v_demo_role_admin, 'identity:*'),
    (v_demo_role_admin, 'crm:*'),
    (v_demo_role_admin, 'sales:*'),
    (v_demo_role_admin, 'work:*'),
    (v_demo_role_admin, 'inventory:*'),
    (v_demo_role_admin, 'finance:*'),
    (v_demo_role_admin, 'notifications:*')
    ON CONFLICT ON CONSTRAINT uk_role_perm DO NOTHING;

INSERT INTO role_permission (role_id, permission_code)
VALUES
    (v_demo_role_user, 'crm:read'),
    (v_demo_role_user, 'sales:read'),
    (v_demo_role_user, 'work:read'),
    (v_demo_role_user, 'inventory:read'),
    (v_demo_role_user, 'finance:read')
    ON CONFLICT ON CONSTRAINT uk_role_perm DO NOTHING;

-- --------------------------------------------------------------------------
-- DEMO: USER owner@demo.local - upsert por email CI
-- --------------------------------------------------------------------------
SELECT id
INTO v_existing_id
FROM app_user
WHERE tenant_id = v_demo_tenant_id
  AND lower(email) = lower('owner@demo.local')
    LIMIT 1;

IF v_existing_id IS NULL THEN
    INSERT INTO app_user (id, tenant_id, email, password_hash, full_name, status, active)
    VALUES (
      v_demo_user_owner,
      v_demo_tenant_id,
      lower('owner@demo.local'),
      v_demo_password_hash,
      'Owner Demo',
      'ACTIVE',
      true
    );
ELSE
    v_demo_user_owner := v_existing_id;

UPDATE app_user
SET email = lower('owner@demo.local'),
    full_name = 'Owner Demo',
    status = 'ACTIVE',
    password_hash = v_demo_password_hash,
    active = true
WHERE id = v_demo_user_owner;
END IF;

INSERT INTO user_role (user_id, role_id)
VALUES
    (v_demo_user_owner, v_demo_role_owner),
    (v_demo_user_owner, v_demo_role_admin)
    ON CONFLICT ON CONSTRAINT uk_user_role DO NOTHING;

END;
$$;
