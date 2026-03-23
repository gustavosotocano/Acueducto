begin;

set search_path to app, public;

-- =========================================================
-- 1) DEMO TENANT
-- =========================================================

insert into app.tenant (
  id,
  tenant_code,
  name,
  identification_number,
  contact_email,
  contact_phone,
  address,
  status
) values (
           '11111111-1111-1111-1111-111111111111',
           'ACUEDUCTO_DEMO',
           'Acueducto Veredal Demo',
           '900000001',
           'admin@acueductodemo.local',
           '3000000000',
           'Vereda Demo',
           'ACTIVE'
         )
on conflict (tenant_code) do nothing;

-- =========================================================
-- 2) DEMO USERS
-- password_hash is placeholder only
-- replace with real bcrypt/argon2 hash in real environments
-- =========================================================

insert into app.app_user (
  id,
  tenant_id,
  full_name,
  email,
  phone,
  password_hash,
  role_code,
  two_factor_method,
  status
) values
    (
      '22222222-2222-2222-2222-222222222221',
      '11111111-1111-1111-1111-111111111111',
      'Super Admin Demo',
      'superadmin@acueductodemo.local',
      '3000000001',
      '$2a$10$demo.hash.replace.in.real.env',
      'SUPER_ADMIN',
      'EMAIL',
      'ACTIVE'
    ),
    (
      '22222222-2222-2222-2222-222222222222',
      '11111111-1111-1111-1111-111111111111',
      'Tenant Admin Demo',
      'tenantadmin@acueductodemo.local',
      '3000000002',
      '$2a$10$demo.hash.replace.in.real.env',
      'TENANT_ADMIN',
      'EMAIL',
      'ACTIVE'
    ),
    (
      '22222222-2222-2222-2222-222222222223',
      '11111111-1111-1111-1111-111111111111',
      'Operator Demo',
      'operator@acueductodemo.local',
      '3000000003',
      '$2a$10$demo.hash.replace.in.real.env',
      'OPERATOR',
      'EMAIL',
      'ACTIVE'
    ),
    (
      '22222222-2222-2222-2222-222222222224',
      '11111111-1111-1111-1111-111111111111',
      'Treasurer Demo',
      'treasurer@acueductodemo.local',
      '3000000004',
      '$2a$10$demo.hash.replace.in.real.env',
      'TREASURER',
      'EMAIL',
      'ACTIVE'
    ),
    (
      '22222222-2222-2222-2222-222222222225',
      '11111111-1111-1111-1111-111111111111',
      'Viewer Demo',
      'viewer@acueductodemo.local',
      '3000000005',
      '$2a$10$demo.hash.replace.in.real.env',
      'VIEWER',
      'EMAIL',
      'ACTIVE'
    )
on conflict (tenant_id, email) do nothing;

-- =========================================================
-- 3) EXPENSE CATEGORIES
-- =========================================================

insert into app.expense_category (
  id,
  tenant_id,
  code,
  name,
  is_active
) values
    (
      '33333333-3333-3333-3333-333333333331',
      '11111111-1111-1111-1111-111111111111',
      'PLUMBER',
      'Fontanero',
      true
    ),
    (
      '33333333-3333-3333-3333-333333333332',
      '11111111-1111-1111-1111-111111111111',
      'MAINTENANCE',
      'Mantenimiento',
      true
    ),
    (
      '33333333-3333-3333-3333-333333333333',
      '11111111-1111-1111-1111-111111111111',
      'STATIONERY',
      'Papelería',
      true
    ),
    (
      '33333333-3333-3333-3333-333333333334',
      '11111111-1111-1111-1111-111111111111',
      'TRANSPORT',
      'Transporte',
      true
    ),
    (
      '33333333-3333-3333-3333-333333333335',
      '11111111-1111-1111-1111-111111111111',
      'ENERGY',
      'Energía',
      true
    ),
    (
      '33333333-3333-3333-3333-333333333336',
      '11111111-1111-1111-1111-111111111111',
      'OTHER',
      'Varios',
      true
    )
on conflict (tenant_id, code) do nothing;

-- =========================================================
-- 4) DEMO OWNERS
-- =========================================================

insert into app.owner (
  id,
  tenant_id,
  identification_type,
  identification_number,
  first_name,
  last_name,
  mobile_phone,
  email,
  address,
  village,
  status
) values
    (
      '44444444-4444-4444-4444-444444444441',
      '11111111-1111-1111-1111-111111111111',
      'CC',
      '100000001',
      'Juan',
      'Pérez',
      '3100000001',
      'juan.perez@example.com',
      'Casa 1',
      'Vereda Central',
      'ACTIVE'
    ),
    (
      '44444444-4444-4444-4444-444444444442',
      '11111111-1111-1111-1111-111111111111',
      'CC',
      '100000002',
      'María',
      'Gómez',
      '3100000002',
      'maria.gomez@example.com',
      'Casa 2',
      'Vereda Central',
      'ACTIVE'
    ),
    (
      '44444444-4444-4444-4444-444444444443',
      '11111111-1111-1111-1111-111111111111',
      'CC',
      '100000003',
      'Carlos',
      'Rodríguez',
      '3100000003',
      'carlos.rodriguez@example.com',
      'Casa 3',
      'Vereda Alta',
      'ACTIVE'
    )
on conflict (tenant_id, identification_type, identification_number) do nothing;

-- =========================================================
-- 5) DEMO WATER RIGHTS
-- =========================================================

insert into app.water_right (
  id,
  tenant_id,
  right_code,
  serial_number,
  current_owner_id,
  current_service_address,
  current_village,
  purchase_date,
  purchase_value,
  status,
  notes
) values
    (
      '55555555-5555-5555-5555-555555555551',
      '11111111-1111-1111-1111-111111111111',
      'WR-0001',
      'SER-0001',
      '44444444-4444-4444-4444-444444444441',
      'Casa 1',
      'Vereda Central',
      current_date - interval '365 days',
      500000.00,
      'ASSIGNED',
      'Derecho demo 1'
    ),
    (
      '55555555-5555-5555-5555-555555555552',
      '11111111-1111-1111-1111-111111111111',
      'WR-0002',
      'SER-0002',
      '44444444-4444-4444-4444-444444444441',
      'Casa 1',
      'Vereda Central',
      current_date - interval '300 days',
      500000.00,
      'ASSIGNED',
      'Derecho demo 2'
    ),
    (
      '55555555-5555-5555-5555-555555555553',
      '11111111-1111-1111-1111-111111111111',
      'WR-0003',
      'SER-0003',
      '44444444-4444-4444-4444-444444444442',
      'Casa 2',
      'Vereda Central',
      current_date - interval '200 days',
      500000.00,
      'ASSIGNED',
      'Derecho demo 3'
    )
on conflict (tenant_id, right_code) do nothing;

-- =========================================================
-- 6) OWNERSHIP HISTORY
-- =========================================================

insert into app.water_right_ownership_history (
  id,
  tenant_id,
  water_right_id,
  owner_id,
  start_date,
  end_date,
  transfer_reason,
  notes,
  registered_by_user_id
) values
    (
      '66666666-6666-6666-6666-666666666661',
      '11111111-1111-1111-1111-111111111111',
      '55555555-5555-5555-5555-555555555551',
      '44444444-4444-4444-4444-444444444441',
      current_date - interval '365 days',
      null,
      'INITIAL_ASSIGNMENT',
      'Asignación inicial',
      '22222222-2222-2222-2222-222222222222'
    ),
    (
      '66666666-6666-6666-6666-666666666662',
      '11111111-1111-1111-1111-111111111111',
      '55555555-5555-5555-5555-555555555552',
      '44444444-4444-4444-4444-444444444441',
      current_date - interval '300 days',
      null,
      'INITIAL_ASSIGNMENT',
      'Asignación inicial',
      '22222222-2222-2222-2222-222222222222'
    ),
    (
      '66666666-6666-6666-6666-666666666663',
      '11111111-1111-1111-1111-111111111111',
      '55555555-5555-5555-5555-555555555553',
      '44444444-4444-4444-4444-444444444442',
      current_date - interval '200 days',
      null,
      'INITIAL_ASSIGNMENT',
      'Asignación inicial',
      '22222222-2222-2222-2222-222222222222'
    )
on conflict do nothing;

-- =========================================================
-- 7) PROPERTY HISTORY
-- =========================================================

insert into app.water_right_property_history (
  id,
  tenant_id,
  water_right_id,
  service_address,
  village,
  start_date,
  end_date,
  change_reason,
  notes,
  registered_by_user_id
) values
    (
      '77777777-7777-7777-7777-777777777771',
      '11111111-1111-1111-1111-111111111111',
      '55555555-5555-5555-5555-555555555551',
      'Casa 1',
      'Vereda Central',
      current_date - interval '365 days',
      null,
      'INITIAL_ASSIGNMENT',
      'Ubicación inicial',
      '22222222-2222-2222-2222-222222222222'
    ),
    (
      '77777777-7777-7777-7777-777777777772',
      '11111111-1111-1111-1111-111111111111',
      '55555555-5555-5555-5555-555555555552',
      'Casa 1',
      'Vereda Central',
      current_date - interval '300 days',
      null,
      'INITIAL_ASSIGNMENT',
      'Ubicación inicial',
      '22222222-2222-2222-2222-222222222222'
    ),
    (
      '77777777-7777-7777-7777-777777777773',
      '11111111-1111-1111-1111-111111111111',
      '55555555-5555-5555-5555-555555555553',
      'Casa 2',
      'Vereda Central',
      current_date - interval '200 days',
      null,
      'INITIAL_ASSIGNMENT',
      'Ubicación inicial',
      '22222222-2222-2222-2222-222222222222'
    )
on conflict do nothing;

-- =========================================================
-- 8) TARIFFS
-- =========================================================

insert into app.tariff (
  id,
  tenant_id,
  concept_code,
  concept_type,
  applies_to,
  name,
  description,
  amount,
  currency_code,
  valid_from,
  valid_to,
  is_active
) values
    (
      '88888888-8888-8888-8888-888888888881',
      '11111111-1111-1111-1111-111111111111',
      'MONTHLY_FEE',
      'MONTHLY_FEE',
      'WATER_RIGHT',
      'Mensualidad',
      'Cobro mensual por derecho de agua',
      25000.00,
      'COP',
      date_trunc('year', current_date)::date,
      null,
      true
    ),
    (
      '88888888-8888-8888-8888-888888888882',
      '11111111-1111-1111-1111-111111111111',
      'WATER_RIGHT_PURCHASE',
      'WATER_RIGHT_PURCHASE',
      'WATER_RIGHT',
      'Compra de derecho',
      'Valor base de compra de derecho',
      500000.00,
      'COP',
      date_trunc('year', current_date)::date,
      null,
      true
    )
on conflict do nothing;

-- =========================================================
-- 9) DEMO INVOICES
-- =========================================================

insert into app.invoice (
  id,
  tenant_id,
  invoice_number,
  water_right_id,
  owner_id,
  billing_year,
  billing_month,
  issue_date,
  due_date,
  status,
  subtotal_amount,
  previous_balance_amount,
  total_amount,
  paid_amount,
  balance_amount,
  generated_at
) values
    (
      '99999999-9999-9999-9999-999999999991',
      '11111111-1111-1111-1111-111111111111',
      'INV-2026-0001',
      '55555555-5555-5555-5555-555555555551',
      '44444444-4444-4444-4444-444444444441',
      2026,
      3,
      current_date - interval '10 days',
      current_date + interval '20 days',
      'PENDING',
      25000.00,
      0.00,
      25000.00,
      0.00,
      25000.00,
      now()
    ),
    (
      '99999999-9999-9999-9999-999999999992',
      '11111111-1111-1111-1111-111111111111',
      'INV-2026-0002',
      '55555555-5555-5555-5555-555555555552',
      '44444444-4444-4444-4444-444444444441',
      2026,
      3,
      current_date - interval '10 days',
      current_date + interval '20 days',
      'PENDING',
      25000.00,
      10000.00,
      35000.00,
      0.00,
      35000.00,
      now()
    ),
    (
      '99999999-9999-9999-9999-999999999993',
      '11111111-1111-1111-1111-111111111111',
      'INV-2026-0003',
      '55555555-5555-5555-5555-555555555553',
      '44444444-4444-4444-4444-444444444442',
      2026,
      3,
      current_date - interval '10 days',
      current_date + interval '20 days',
      'PENDING',
      25000.00,
      0.00,
      25000.00,
      0.00,
      25000.00,
      now()
    )
on conflict do nothing;

insert into app.invoice_item (
  id,
  tenant_id,
  invoice_id,
  concept_code,
  concept_name,
  quantity,
  unit_amount,
  total_amount
) values
    (
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
      '11111111-1111-1111-1111-111111111111',
      '99999999-9999-9999-9999-999999999991',
      'MONTHLY_FEE',
      'Mensualidad',
      1,
      25000.00,
      25000.00
    ),
    (
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
      '11111111-1111-1111-1111-111111111111',
      '99999999-9999-9999-9999-999999999992',
      'MONTHLY_FEE',
      'Mensualidad',
      1,
      25000.00,
      25000.00
    ),
    (
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
      '11111111-1111-1111-1111-111111111111',
      '99999999-9999-9999-9999-999999999992',
      'PREVIOUS_BALANCE',
      'Saldo anterior',
      1,
      10000.00,
      10000.00
    ),
    (
      'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4',
      '11111111-1111-1111-1111-111111111111',
      '99999999-9999-9999-9999-999999999993',
      'MONTHLY_FEE',
      'Mensualidad',
      1,
      25000.00,
      25000.00
    )
on conflict do nothing;

-- =========================================================
-- 10) DEMO PAYMENT + RECEIPT
-- =========================================================

insert into app.payment (
  id,
  tenant_id,
  owner_id,
  payment_date,
  amount,
  payment_method,
  reference,
  notes,
  received_by_user_id
) values
  (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444441',
    current_date,
    25000.00,
    'CASH',
    'PAY-DEMO-0001',
    'Pago demo',
    '22222222-2222-2222-2222-222222222223'
  )
on conflict do nothing;

insert into app.payment_application (
  id,
  tenant_id,
  payment_id,
  invoice_id,
  water_right_id,
  applied_amount
) values
  (
    'cccccccc-cccc-cccc-cccc-ccccccccccc1',
    '11111111-1111-1111-1111-111111111111',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    '99999999-9999-9999-9999-999999999991',
    '55555555-5555-5555-5555-555555555551',
    25000.00
  )
on conflict do nothing;

insert into app.cash_receipt (
  id,
  tenant_id,
  receipt_number,
  payment_id,
  issue_date,
  total_received
) values
  (
    'dddddddd-dddd-dddd-dddd-ddddddddddd1',
    '11111111-1111-1111-1111-111111111111',
    'RCPT-2026-0001',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    current_date,
    25000.00
  )
on conflict do nothing;

-- =========================================================
-- 11) DEMO EXPENSES
-- =========================================================

insert into app.expense (
  id,
  tenant_id,
  expense_date,
  expense_category_id,
  description,
  supplier_name,
  amount,
  support_file_url,
  status,
  created_by_user_id
) values
    (
      'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
      '11111111-1111-1111-1111-111111111111',
      current_date - interval '5 days',
      '33333333-3333-3333-3333-333333333331',
      'Pago de fontanero',
      'Pedro Fontanero',
      80000.00,
      null,
      'ACTIVE',
      '22222222-2222-2222-2222-222222222224'
    ),
    (
      'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2',
      '11111111-1111-1111-1111-111111111111',
      current_date - interval '3 days',
      '33333333-3333-3333-3333-333333333333',
      'Compra de papelería',
      'Papelería Central',
      25000.00,
      null,
      'ACTIVE',
      '22222222-2222-2222-2222-222222222224'
    )
on conflict do nothing;

-- =========================================================
-- 12) DEMO MONTHLY CLOSING
-- =========================================================

insert into app.monthly_closing (
  id,
  tenant_id,
  closing_year,
  closing_month,
  invoiced_total,
  collected_total,
  expense_total,
  outstanding_portfolio_total,
  net_balance_total,
  generated_by_user_id,
  generated_at
) values
  (
    'ffffffff-ffff-ffff-ffff-fffffffffff1',
    '11111111-1111-1111-1111-111111111111',
    2026,
    3,
    85000.00,
    25000.00,
    105000.00,
    60000.00,
    -80000.00,
    '22222222-2222-2222-2222-222222222224',
    now()
  )
on conflict do nothing;

-- =========================================================
-- 13) DEMO AUDIT
-- =========================================================

insert into app.audit_event (
  id,
  tenant_id,
  event_type,
  entity_name,
  entity_id,
  performed_by_user_id,
  event_timestamp,
  event_data
) values
  (
    '12121212-1212-1212-1212-121212121212',
    '11111111-1111-1111-1111-111111111111',
    'WATER_RIGHT_CREATED',
    'water_right',
    '55555555-5555-5555-5555-555555555551',
    '22222222-2222-2222-2222-222222222222',
    now(),
    jsonb_build_object(
      'rightCode', 'WR-0001',
      'ownerId', '44444444-4444-4444-4444-444444444441',
      'note', 'Initial seed data'
    )
  )
on conflict do nothing;

commit;
