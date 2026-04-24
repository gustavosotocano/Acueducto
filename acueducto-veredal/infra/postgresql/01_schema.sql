begin;

create extension if not exists pgcrypto;
create extension if not exists btree_gist;

create schema if not exists app;

set search_path to app, public;

-- =========================================================
-- 1) COMMON FUNCTIONS
-- =========================================================

create or replace function app.set_updated_at()
  returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

create or replace function app.refresh_water_right_current_state(p_tenant_id uuid, p_water_right_id uuid)
  returns void
language plpgsql
as $$
declare
  v_owner_id uuid;
  v_service_address varchar(255);
  v_village varchar(120);
begin
  select h.owner_id
  into v_owner_id
  from app.water_right_ownership_history h
  where h.tenant_id = p_tenant_id
    and h.water_right_id = p_water_right_id
    and h.end_date is null
  order by h.start_date desc, h.created_at desc
  limit 1;

  select h.service_address, h.village
  into v_service_address, v_village
  from app.water_right_property_history
  where h.tenant_id = p_tenant_id
    and h.water_right_id = p_water_right_id
    and h.end_date is null
  order by h.start_date desc, h.created_at desc
  limit 1;

  update app.water_right
  set current_owner_id = v_owner_id,
      current_service_address = v_service_address,
      current_village = v_village,
      updated_at = now()
  where tenant_id = p_tenant_id
    and id = p_water_right_id;
end;
$$;

create or replace function app.trg_refresh_water_right_from_ownership()
  returns trigger
language plpgsql
as $$
declare
  v_tenant_id uuid;
  v_water_right_id uuid;
begin
  v_tenant_id := coalesce(new.tenant_id, old.tenant_id);
  v_water_right_id := coalesce(new.water_right_id, old.water_right_id);

  perform app.refresh_water_right_current_state(v_tenant_id, v_water_right_id);
  return null;
end;
$$;

create or replace function app.trg_refresh_water_right_from_property()
  returns trigger
language plpgsql
as $$
declare
  v_tenant_id uuid;
  v_water_right_id uuid;
begin
  v_tenant_id := coalesce(new.tenant_id, old.tenant_id);
  v_water_right_id := coalesce(new.water_right_id, old.water_right_id);

  perform app.refresh_water_right_current_state(v_tenant_id, v_water_right_id);
  return null;
end;
$$;

create or replace function app.validate_payment_application()
  returns trigger
language plpgsql
as $$
declare
  v_payment_amount numeric(14,2);
  v_applied_total numeric(14,2);
  v_invoice_balance numeric(14,2);
  v_payment_tenant_id uuid;
  v_invoice_tenant_id uuid;
begin
  select p.amount, p.tenant_id
  into v_payment_amount, v_payment_tenant_id
  from app.payment p
  where p.id = new.payment_id
    and p.tenant_id = new.tenant_id;

  if v_payment_amount is null then
    raise exception 'payment % not found for tenant %', new.payment_id, new.tenant_id;
  end if;

  select i.balance_amount, i.tenant_id
  into v_invoice_balance, v_invoice_tenant_id
  from app.invoice i
  where i.id = new.invoice_id
    and i.tenant_id = new.tenant_id;

  if v_invoice_balance is null then
    raise exception 'invoice % not found for tenant %', new.invoice_id, new.tenant_id;
  end if;

  if v_payment_tenant_id <> v_invoice_tenant_id then
    raise exception 'payment and invoice must belong to same tenant';
  end if;

  if new.applied_amount <= 0 then
    raise exception 'applied_amount must be greater than 0';
  end if;

  select coalesce(sum(pa.applied_amount), 0)
  into v_applied_total
  from app.payment_application pa
  where pa.tenant_id = new.tenant_id
    and pa.payment_id = new.payment_id
    and pa.id <> coalesce(new.id, gen_random_uuid());

  v_applied_total := v_applied_total + new.applied_amount;

  if v_applied_total > v_payment_amount then
    raise exception 'applied amount exceeds payment amount. payment=%, applied_total=%',
            v_payment_amount, v_applied_total;
  end if;

  if new.applied_amount > v_invoice_balance then
    raise exception 'applied amount exceeds current invoice balance. invoice_balance=%, applied_amount=%',
            v_invoice_balance, new.applied_amount;
  end if;

  return new;
end;
$$;

create or replace function app.recalculate_invoice_amounts(p_tenant_id uuid, p_invoice_id uuid)
  returns void
language plpgsql
as $$
declare
  v_total_applied numeric(14,2);
  v_total_amount numeric(14,2);
begin
  select i.total_amount
  into v_total_amount
  from app.invoice i
  where i.tenant_id = p_tenant_id
    and i.id = p_invoice_id;

  select coalesce(sum(pa.applied_amount), 0)
  into v_total_applied
  from app.payment_application pa
  where pa.tenant_id = p_tenant_id
    and pa.invoice_id = p_invoice_id;

  update app.invoice
  set paid_amount = v_total_applied,
      balance_amount = v_total_amount - v_total_applied,
      status = case
                 when v_total_applied = 0 then status
                 when v_total_applied < v_total_amount then 'PARTIALLY_PAID'
                 when v_total_applied >= v_total_amount then 'PAID'
        end,
      updated_at = now()
  where tenant_id = p_tenant_id
    and id = p_invoice_id;
end;
$$;

create or replace function app.trg_recalculate_invoice_after_payment_application()
  returns trigger
language plpgsql
as $$
declare
  v_tenant_id uuid;
  v_invoice_id uuid;
begin
  v_tenant_id := coalesce(new.tenant_id, old.tenant_id);
  v_invoice_id := coalesce(new.invoice_id, old.invoice_id);

  perform app.recalculate_invoice_amounts(v_tenant_id, v_invoice_id);
  return null;
end;
$$;

-- =========================================================
-- 2) MASTER TABLES
-- =========================================================

create table app.tenant (
                          id uuid primary key default gen_random_uuid(),
                          tenant_code varchar(50) not null,
                          name varchar(150) not null,
                          identification_number varchar(50),
                          contact_email varchar(150),
                          contact_phone varchar(30),
                          address varchar(255),
                          status varchar(30) not null,
                          created_at timestamptz not null default now(),
                          updated_at timestamptz not null default now(),

                          constraint uq_tenant_code unique (tenant_code),
                          constraint ck_tenant_status check (status in ('ACTIVE', 'INACTIVE'))
);

create table app.app_user (
                            id uuid primary key default gen_random_uuid(),
                            tenant_id uuid not null,
                            full_name varchar(150) not null,
                            email varchar(150) not null,
                            phone varchar(30),
                            password_hash varchar(255) not null,
                            role_code varchar(50) not null,
                            two_factor_method varchar(30),
                            status varchar(30) not null,
                            last_login_at timestamptz,
                            created_at timestamptz not null default now(),
                            updated_at timestamptz not null default now(),

                            constraint fk_app_user_tenant
                              foreign key (tenant_id) references app.tenant(id),

                            constraint uq_app_user_tenant_email unique (tenant_id, email),
                            constraint uq_app_user_tenant_id unique (tenant_id, id),

                            constraint ck_app_user_role_code check (
                              role_code in ('SUPER_ADMIN', 'TENANT_ADMIN', 'OPERATOR', 'TREASURER', 'VIEWER')
                              ),
                            constraint ck_app_user_two_factor_method check (
                              two_factor_method is null or two_factor_method in ('EMAIL', 'SMS', 'WHATSAPP')
                              ),
                            constraint ck_app_user_status check (
                              status in ('ACTIVE', 'INACTIVE', 'LOCKED')
                              )
);

create trigger trg_app_user_set_updated_at
  before update on app.app_user
  for each row
  execute function app.set_updated_at();

create table app.owner (
                         id uuid primary key default gen_random_uuid(),
                         tenant_id uuid not null,
                         identification_type varchar(30),
                         identification_number varchar(50) not null,
                         first_name varchar(100) not null,
                         last_name varchar(100) not null,
                         mobile_phone varchar(30),
                         email varchar(150),
                         address varchar(255),
                         village varchar(120),
                         status varchar(30) not null,
                         created_at timestamptz not null default now(),
                         updated_at timestamptz not null default now(),

                         constraint fk_owner_tenant
                           foreign key (tenant_id) references app.tenant(id),

                         constraint uq_owner_tenant_identification
                           unique (tenant_id, identification_type, identification_number),

                         constraint uq_owner_tenant_id unique (tenant_id, id),

                         constraint ck_owner_status check (
                           status in ('ACTIVE', 'INACTIVE')
                           )
);

create trigger trg_owner_set_updated_at
  before update on app.owner
  for each row
  execute function app.set_updated_at();

create table app.water_right (
                               id uuid primary key default gen_random_uuid(),
                               tenant_id uuid not null,
                               right_code varchar(50) not null,
                               serial_number varchar(50),
                               current_owner_id uuid,
                               current_service_address varchar(255),
                               current_village varchar(120),
                               purchase_date date,
                               purchase_value numeric(14,2),
                               status varchar(30) not null,
                               notes text,
                               created_at timestamptz not null default now(),
                               updated_at timestamptz not null default now(),

                               constraint fk_water_right_tenant
                                 foreign key (tenant_id) references app.tenant(id),

                               constraint fk_water_right_current_owner
                                 foreign key (tenant_id, current_owner_id)
                                   references app.owner(tenant_id, id)
                                   deferrable initially immediate,

                               constraint uq_water_right_tenant_code
                                 unique (tenant_id, right_code),

                               constraint uq_water_right_tenant_id unique (tenant_id, id),

                               constraint ck_water_right_status check (
                                 status in ('AVAILABLE', 'ASSIGNED', 'SUSPENDED', 'INACTIVE')
                                 ),
                               constraint ck_water_right_purchase_value check (
                                 purchase_value is null or purchase_value >= 0
                                 )
);

create unique index uq_water_right_tenant_serial_number
  on app.water_right (tenant_id, serial_number)
  where serial_number is not null;

create trigger trg_water_right_set_updated_at
  before update on app.water_right
  for each row
  execute function app.set_updated_at();

-- =========================================================
-- 3) HISTORY TABLES
-- =========================================================

create table app.water_right_ownership_history (
                                                 id uuid primary key default gen_random_uuid(),
                                                 tenant_id uuid not null,
                                                 water_right_id uuid not null,
                                                 owner_id uuid not null,
                                                 start_date date not null,
                                                 end_date date,
                                                 transfer_reason varchar(255),
                                                 notes text,
                                                 registered_by_user_id uuid,
                                                 created_at timestamptz not null default now(),

                                                 constraint fk_wr_ownership_history_tenant
                                                   foreign key (tenant_id) references app.tenant(id),

                                                 constraint fk_wr_ownership_history_water_right
                                                   foreign key (tenant_id, water_right_id)
                                                     references app.water_right(tenant_id, id)
                                                     deferrable initially immediate,

                                                 constraint fk_wr_ownership_history_owner
                                                   foreign key (tenant_id, owner_id)
                                                     references app.owner(tenant_id, id)
                                                     deferrable initially immediate,

                                                 constraint fk_wr_ownership_history_registered_by
                                                   foreign key (tenant_id, registered_by_user_id)
                                                     references app.app_user(tenant_id, id)
                                                     deferrable initially immediate,

                                                 constraint uq_wr_ownership_history_tenant_id unique (tenant_id, id),

                                                 constraint ck_wr_ownership_dates check (
                                                   end_date is null or start_date <= end_date
                                                   )
);

create unique index uq_wr_ownership_one_active
  on app.water_right_ownership_history (tenant_id, water_right_id)
  where end_date is null;

alter table app.water_right_ownership_history
  add constraint ex_wr_ownership_no_overlap
  exclude using gist (
  tenant_id with =,
  water_right_id with =,
  daterange(start_date, coalesce(end_date, 'infinity'::date), '[]') with &&
  );

create index ix_wr_ownership_owner
  on app.water_right_ownership_history (tenant_id, owner_id, start_date desc);

create trigger trg_refresh_wr_from_ownership_ins_upd_del
  after insert or update or delete on app.water_right_ownership_history
  for each row
execute function app.trg_refresh_water_right_from_ownership();

create table app.water_right_property_history (
                                                id uuid primary key default gen_random_uuid(),
                                                tenant_id uuid not null,
                                                water_right_id uuid not null,
                                                service_address varchar(255) not null,
                                                village varchar(120),
                                                start_date date not null,
                                                end_date date,
                                                change_reason varchar(255),
                                                notes text,
                                                registered_by_user_id uuid,
                                                created_at timestamptz not null default now(),

                                                constraint fk_wr_property_history_tenant
                                                  foreign key (tenant_id) references app.tenant(id),

                                                constraint fk_wr_property_history_water_right
                                                  foreign key (tenant_id, water_right_id)
                                                    references app.water_right(tenant_id, id)
                                                    deferrable initially immediate,

                                                constraint fk_wr_property_history_registered_by
                                                  foreign key (tenant_id, registered_by_user_id)
                                                    references app.app_user(tenant_id, id)
                                                    deferrable initially immediate,

                                                constraint uq_wr_property_history_tenant_id unique (tenant_id, id),

                                                constraint ck_wr_property_dates check (
                                                  end_date is null or start_date <= end_date
                                                  )
);

create unique index uq_wr_property_one_active
  on app.water_right_property_history (tenant_id, water_right_id)
  where end_date is null;

alter table app.water_right_property_history
  add constraint ex_wr_property_no_overlap
  exclude using gist (
  tenant_id with =,
  water_right_id with =,
  daterange(start_date, coalesce(end_date, 'infinity'::date), '[]') with &&
  );

create index ix_wr_property_history_dates
  on app.water_right_property_history (tenant_id, water_right_id, start_date desc);

create trigger trg_refresh_wr_from_property_ins_upd_del
  after insert or update or delete on app.water_right_property_history
  for each row
execute function app.trg_refresh_water_right_from_property();

-- =========================================================
-- 4) BILLING / TARIFFS
-- =========================================================

create table app.tariff (
                          id uuid primary key default gen_random_uuid(),
                          tenant_id uuid not null,
                          concept_code varchar(50) not null,
                          concept_type varchar(50) not null,
                          applies_to varchar(30) not null,
                          name varchar(150) not null,
                          description varchar(255),
                          amount numeric(14,2) not null,
                          currency_code varchar(10) not null default 'COP',
                          valid_from date not null,
                          valid_to date,
                          is_active boolean not null default true,
                          created_at timestamptz not null default now(),
                          updated_at timestamptz not null default now(),

                          constraint fk_tariff_tenant
                            foreign key (tenant_id) references app.tenant(id),

                          constraint uq_tariff_tenant_id unique (tenant_id, id),

                          constraint ck_tariff_amount check (amount >= 0),
                          constraint ck_tariff_dates check (
                            valid_to is null or valid_from <= valid_to
                            ),
                          constraint ck_tariff_applies_to check (
                            applies_to in ('WATER_RIGHT', 'OWNER', 'GENERAL')
                            ),
                          constraint ck_tariff_concept_type check (
                            concept_type in ('MONTHLY_FEE', 'WATER_RIGHT_PURCHASE', 'RECONNECTION', 'ADJUSTMENT', 'OTHER')
                            )
);

create trigger trg_tariff_set_updated_at
  before update on app.tariff
  for each row
  execute function app.set_updated_at();

alter table app.tariff
  add constraint ex_tariff_no_overlap
  exclude using gist (
  tenant_id with =,
  concept_code with =,
  daterange(valid_from, coalesce(valid_to, 'infinity'::date), '[]') with &&
  )
  where (is_active);

create index ix_tariff_lookup
  on app.tariff (tenant_id, concept_code, valid_from desc);

create table app.invoice (
                           id uuid primary key default gen_random_uuid(),
                           tenant_id uuid not null,
                           invoice_number varchar(50) not null,
                           water_right_id uuid not null,
                           owner_id uuid not null,
                           billing_year integer not null,
                           billing_month integer not null,
                           issue_date date not null,
                           due_date date not null,
                           status varchar(30) not null,
                           subtotal_amount numeric(14,2) not null,
                           previous_balance_amount numeric(14,2) not null default 0,
                           total_amount numeric(14,2) not null,
                           paid_amount numeric(14,2) not null default 0,
                           balance_amount numeric(14,2) not null,
                           generated_at timestamptz not null default now(),
                           created_at timestamptz not null default now(),
                           updated_at timestamptz not null default now(),

                           constraint fk_invoice_tenant
                             foreign key (tenant_id) references app.tenant(id),

                           constraint fk_invoice_water_right
                             foreign key (tenant_id, water_right_id)
                               references app.water_right(tenant_id, id)
                               deferrable initially immediate,

                           constraint fk_invoice_owner
                             foreign key (tenant_id, owner_id)
                               references app.owner(tenant_id, id)
                               deferrable initially immediate,

                           constraint uq_invoice_tenant_number
                             unique (tenant_id, invoice_number),

                           constraint uq_invoice_tenant_period
                             unique (tenant_id, water_right_id, billing_year, billing_month),

                           constraint uq_invoice_tenant_id unique (tenant_id, id),

                           constraint ck_invoice_billing_year check (billing_year between 2000 and 3000),
                           constraint ck_invoice_billing_month check (billing_month between 1 and 12),
                           constraint ck_invoice_due_date check (due_date >= issue_date),
                           constraint ck_invoice_amounts_non_negative check (
                             subtotal_amount >= 0
                               and previous_balance_amount >= 0
                               and total_amount >= 0
                               and paid_amount >= 0
                               and balance_amount >= 0
                             ),
                           constraint ck_invoice_totals check (
                             total_amount = subtotal_amount + previous_balance_amount
                               and balance_amount = total_amount - paid_amount
                             ),
                           constraint ck_invoice_status check (
                             status in ('PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED')
                             )
);

create trigger trg_invoice_set_updated_at
  before update on app.invoice
  for each row
  execute function app.set_updated_at();

create index ix_invoice_owner_status
  on app.invoice (tenant_id, owner_id, status, billing_year desc, billing_month desc);

create index ix_invoice_water_right_period
  on app.invoice (tenant_id, water_right_id, billing_year desc, billing_month desc);

create index ix_invoice_due_date
  on app.invoice (tenant_id, due_date, status);

create table app.invoice_item (
                                id uuid primary key default gen_random_uuid(),
                                tenant_id uuid not null,
                                invoice_id uuid not null,
                                concept_code varchar(50) not null,
                                concept_name varchar(150) not null,
                                quantity numeric(12,2) not null default 1,
                                unit_amount numeric(14,2) not null,
                                total_amount numeric(14,2) not null,
                                created_at timestamptz not null default now(),

                                constraint fk_invoice_item_tenant
                                  foreign key (tenant_id) references app.tenant(id),

                                constraint fk_invoice_item_invoice
                                  foreign key (tenant_id, invoice_id)
                                    references app.invoice(tenant_id, id)
                                    on delete cascade
                                    deferrable initially immediate,

                                constraint ck_invoice_item_quantity check (quantity > 0),
                                constraint ck_invoice_item_unit_amount check (unit_amount >= 0),
                                constraint ck_invoice_item_total_amount check (
                                  total_amount >= 0
                                    and total_amount = quantity * unit_amount
                                  )
);

create index ix_invoice_item_invoice
  on app.invoice_item (tenant_id, invoice_id);

-- =========================================================
-- 5) PAYMENTS / RECEIPTS
-- =========================================================

create table app.payment (
                           id uuid primary key default gen_random_uuid(),
                           tenant_id uuid not null,
                           owner_id uuid not null,
                           payment_date date not null,
                           amount numeric(14,2) not null,
                           payment_method varchar(30) not null,
                           reference varchar(100),
                           notes text,
                           received_by_user_id uuid,
                           created_at timestamptz not null default now(),
                           updated_at timestamptz not null default now(),

                           constraint fk_payment_tenant
                             foreign key (tenant_id) references app.tenant(id),

                           constraint fk_payment_owner
                             foreign key (tenant_id, owner_id)
                               references app.owner(tenant_id, id)
                               deferrable initially immediate,

                           constraint fk_payment_received_by
                             foreign key (tenant_id, received_by_user_id)
                               references app.app_user(tenant_id, id)
                               deferrable initially immediate,

                           constraint uq_payment_tenant_id unique (tenant_id, id),

                           constraint ck_payment_amount check (amount > 0),
                           constraint ck_payment_method check (
                             payment_method in ('CASH', 'BANK_TRANSFER', 'CARD', 'NEQUI', 'DAVIPLATA', 'OTHER')
                             )
);

create trigger trg_payment_set_updated_at
  before update on app.payment
  for each row
  execute function app.set_updated_at();

create index ix_payment_owner_date
  on app.payment (tenant_id, owner_id, payment_date desc);

create table app.payment_application (
                                       id uuid primary key default gen_random_uuid(),
                                       tenant_id uuid not null,
                                       payment_id uuid not null,
                                       invoice_id uuid not null,
                                       water_right_id uuid not null,
                                       applied_amount numeric(14,2) not null,
                                       created_at timestamptz not null default now(),

                                       constraint fk_payment_application_tenant
                                         foreign key (tenant_id) references app.tenant(id),

                                       constraint fk_payment_application_payment
                                         foreign key (tenant_id, payment_id)
                                           references app.payment(tenant_id, id)
                                           on delete cascade
                                           deferrable initially immediate,

                                       constraint fk_payment_application_invoice
                                         foreign key (tenant_id, invoice_id)
                                           references app.invoice(tenant_id, id)
                                           on delete cascade
                                           deferrable initially immediate,

                                       constraint fk_payment_application_water_right
                                         foreign key (tenant_id, water_right_id)
                                           references app.water_right(tenant_id, id)
                                           deferrable initially immediate,

                                       constraint ck_payment_application_amount check (applied_amount > 0)
);

create index ix_payment_application_payment
  on app.payment_application (tenant_id, payment_id);

create index ix_payment_application_invoice
  on app.payment_application (tenant_id, invoice_id);

create trigger trg_validate_payment_application
  before insert or update on app.payment_application
  for each row
execute function app.validate_payment_application();

create trigger trg_recalculate_invoice_after_payment_application
  after insert or update or delete on app.payment_application
  for each row
execute function app.trg_recalculate_invoice_after_payment_application();

create table app.cash_receipt (
                                id uuid primary key default gen_random_uuid(),
                                tenant_id uuid not null,
                                receipt_number varchar(50) not null,
                                payment_id uuid not null,
                                issue_date date not null,
                                total_received numeric(14,2) not null,
                                created_at timestamptz not null default now(),

                                constraint fk_cash_receipt_tenant
                                  foreign key (tenant_id) references app.tenant(id),

                                constraint fk_cash_receipt_payment
                                  foreign key (tenant_id, payment_id)
                                    references app.payment(tenant_id, id)
                                    deferrable initially immediate,

                                constraint uq_cash_receipt_tenant_number
                                  unique (tenant_id, receipt_number),

                                constraint uq_cash_receipt_payment unique (tenant_id, payment_id),

                                constraint ck_cash_receipt_total_received check (total_received > 0)
);

create index ix_cash_receipt_issue_date
  on app.cash_receipt (tenant_id, issue_date desc);

-- =========================================================
-- 6) EXPENSES / CLOSING
-- =========================================================

create table app.expense_category (
                                    id uuid primary key default gen_random_uuid(),
                                    tenant_id uuid not null,
                                    code varchar(50) not null,
                                    name varchar(150) not null,
                                    is_active boolean not null default true,
                                    created_at timestamptz not null default now(),
                                    updated_at timestamptz not null default now(),

                                    constraint fk_expense_category_tenant
                                      foreign key (tenant_id) references app.tenant(id),

                                    constraint uq_expense_category_tenant_code unique (tenant_id, code),
                                    constraint uq_expense_category_tenant_id unique (tenant_id, id)
);

create trigger trg_expense_category_set_updated_at
  before update on app.expense_category
  for each row
  execute function app.set_updated_at();

create table app.expense (
                           id uuid primary key default gen_random_uuid(),
                           tenant_id uuid not null,
                           expense_date date not null,
                           expense_category_id uuid,
                           description varchar(255) not null,
                           supplier_name varchar(150),
                           amount numeric(14,2) not null,
                           support_file_url varchar(500),
                           status varchar(30) not null,
                           created_by_user_id uuid,
                           created_at timestamptz not null default now(),
                           updated_at timestamptz not null default now(),

                           constraint fk_expense_tenant
                             foreign key (tenant_id) references app.tenant(id),

                           constraint fk_expense_category
                             foreign key (tenant_id, expense_category_id)
                               references app.expense_category(tenant_id, id)
                               deferrable initially immediate,

                           constraint fk_expense_created_by
                             foreign key (tenant_id, created_by_user_id)
                               references app.app_user(tenant_id, id)
                               deferrable initially immediate,

                           constraint ck_expense_amount check (amount > 0),
                           constraint ck_expense_status check (
                             status in ('ACTIVE', 'CANCELLED')
                             )
);

create trigger trg_expense_set_updated_at
  before update on app.expense
  for each row
  execute function app.set_updated_at();

create index ix_expense_date
  on app.expense (tenant_id, expense_date desc);

create index ix_expense_status
  on app.expense (tenant_id, status, expense_date desc);

create table app.monthly_closing (
                                   id uuid primary key default gen_random_uuid(),
                                   tenant_id uuid not null,
                                   closing_year integer not null,
                                   closing_month integer not null,
                                   invoiced_total numeric(14,2) not null default 0,
                                   collected_total numeric(14,2) not null default 0,
                                   expense_total numeric(14,2) not null default 0,
                                   outstanding_portfolio_total numeric(14,2) not null default 0,
                                   net_balance_total numeric(14,2) not null default 0,
                                   generated_by_user_id uuid,
                                   generated_at timestamptz not null default now(),

                                   constraint fk_monthly_closing_tenant
                                     foreign key (tenant_id) references app.tenant(id),

                                   constraint fk_monthly_closing_generated_by
                                     foreign key (tenant_id, generated_by_user_id)
                                       references app.app_user(tenant_id, id)
                                       deferrable initially immediate,

                                   constraint uq_monthly_closing_period
                                     unique (tenant_id, closing_year, closing_month),

                                   constraint ck_monthly_closing_year check (closing_year between 2000 and 3000),
                                   constraint ck_monthly_closing_month check (closing_month between 1 and 12),
                                   constraint ck_monthly_closing_amounts check (
                                     invoiced_total >= 0
                                       and collected_total >= 0
                                       and expense_total >= 0
                                       and outstanding_portfolio_total >= 0
                                     )
);

create index ix_monthly_closing_period
  on app.monthly_closing (tenant_id, closing_year desc, closing_month desc);

-- =========================================================
-- 7) AUDIT
-- =========================================================

create table app.audit_event (
                               id uuid primary key default gen_random_uuid(),
                               tenant_id uuid not null,
                               event_type varchar(100) not null,
                               entity_name varchar(100) not null,
                               entity_id uuid not null,
                               performed_by_user_id uuid,
                               event_timestamp timestamptz not null default now(),
                               event_data jsonb,

                               constraint fk_audit_event_tenant
                                 foreign key (tenant_id) references app.tenant(id),

                               constraint fk_audit_event_performed_by
                                 foreign key (tenant_id, performed_by_user_id)
                                   references app.app_user(tenant_id, id)
                                   deferrable initially immediate
);

create index ix_audit_event_entity
  on app.audit_event (tenant_id, entity_name, entity_id, event_timestamp desc);

create index ix_audit_event_type
  on app.audit_event (tenant_id, event_type, event_timestamp desc);

create index ix_audit_event_data_gin
  on app.audit_event using gin (event_data);

-- =========================================================
-- 8) SEARCH / OPERATIONAL INDEXES
-- =========================================================

create index ix_owner_identification
  on app.owner (tenant_id, identification_number);

create index ix_owner_name
  on app.owner (tenant_id, last_name, first_name);

create index ix_owner_status
  on app.owner (tenant_id, status);

create index ix_water_right_current_owner
  on app.water_right (tenant_id, current_owner_id);

create index ix_water_right_status
  on app.water_right (tenant_id, status);

create index ix_water_right_address
  on app.water_right (tenant_id, current_service_address);

-- =========================================================
-- 9) OPTIONAL SEED CATALOGS
-- =========================================================

insert into app.expense_category (tenant_id, code, name)
select t.id, 'PLUMBER', 'Plumber'
from app.tenant t
where false;

commit;
