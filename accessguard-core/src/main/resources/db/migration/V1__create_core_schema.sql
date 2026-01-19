-- Core DB schema (accessguard-core)
-- Database: accessguard_core

create table if not exists zones (
    id uuid primary key,
    tenant_id varchar(64) not null,
    zone_code varchar(64) not null,
    parent_zone_id uuid null,
    name varchar(255) null,
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    constraint uq_zones_tenant_code unique (tenant_id, zone_code),
    constraint fk_zones_parent foreign key (parent_zone_id) references zones(id),
    constraint ck_zones_status check (status in ('ACTIVE', 'INACTIVE'))
);
create index if not exists ix_zones_tenant_parent on zones (tenant_id, parent_zone_id);
create index if not exists ix_zones_tenant_status on zones (tenant_id, status);

create table if not exists doors (
    id uuid primary key,
    tenant_id varchar(64) not null,
    door_code varchar(64) not null,
    zone_id uuid not null,
    name varchar(255) null,
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    constraint uq_doors_tenant_code unique (tenant_id, door_code),
    constraint fk_doors_zone foreign key (zone_id) references zones(id),
    constraint ck_doors_status check (status in ('ACTIVE', 'INACTIVE'))
);
create index if not exists ix_doors_tenant_zone on doors (tenant_id, zone_id);
create index if not exists ix_doors_tenant_status on doors (tenant_id, status);

create table if not exists devices (
    id uuid primary key,
    tenant_id varchar(64) not null,
    device_code varchar(64) not null,
    status varchar(32) not null default 'ACTIVE',
    name varchar(255) null,
    created_at timestamptz not null default now(),
    constraint uq_devices_tenant_code unique (tenant_id, device_code),
    constraint ck_devices_status check (status in ('ACTIVE', 'INACTIVE'))
);
create index if not exists ix_devices_tenant_status on devices (tenant_id, status);

create table if not exists passes (
    id uuid primary key,
    tenant_id varchar(64) not null,
    pass_code varchar(64) not null,
    status varchar(32) not null,
    visitor_ref varchar(500) not null,
    valid_from timestamptz not null,
    valid_to timestamptz not null,
    revoked_at timestamptz null,
    revoked_reason varchar(500) null,
    revoked_by varchar(128) null,
    created_at timestamptz not null default now(),
    created_by varchar(128) not null,
    updated_at timestamptz not null default now(),
    updated_by varchar(128) not null,
    constraint uq_passes_tenant_pass_code unique (tenant_id, pass_code),
    constraint ck_pass_valid_window check (valid_from < valid_to),
    constraint ck_pass_status check (status in ('ACTIVE', 'REVOKED'))
);
create index if not exists ix_passes_tenant_status on passes (tenant_id, status);
create index if not exists ix_passes_tenant_valid_to on passes (tenant_id, valid_to);

create table if not exists pass_scope_doors (
    pass_id uuid not null,
    tenant_id varchar(64) not null,
    door_id uuid not null,
    created_at timestamptz not null default now(),
    created_by varchar(128) not null,
    constraint pk_pass_scope_doors primary key (pass_id, door_id),
    constraint fk_psd_pass foreign key (pass_id) references passes(id) on delete cascade,
    constraint fk_psd_door foreign key (door_id) references doors(id) on delete restrict
);
create index if not exists ix_psd_tenant_pass on pass_scope_doors (tenant_id, pass_id);
create index if not exists ix_psd_tenant_door on pass_scope_doors (tenant_id, door_id);

create table if not exists pass_scope_zones (
    pass_id uuid not null,
    tenant_id varchar(64) not null,
    zone_id uuid not null,
    created_at timestamptz not null default now(),
    created_by varchar(128) not null,
    constraint pk_pass_scope_zones primary key (pass_id, zone_id),
    constraint fk_psz_pass foreign key (pass_id) references passes(id) on delete cascade,
    constraint fk_psz_zone foreign key (zone_id) references zones(id) on delete restrict
);
create index if not exists ix_psz_tenant_pass on pass_scope_zones (tenant_id, pass_id);
create index if not exists ix_psz_tenant_zone on pass_scope_zones (tenant_id, zone_id);

create table if not exists access_attempts (
    id uuid primary key,
    tenant_id varchar(64) not null,
    attempt_id uuid not null,
    device_id uuid not null,
    door_id uuid not null,
    pass_id uuid null,
    decision varchar(16) not null,
    reason_code varchar(64) not null,
    occurred_at timestamptz not null,
    evaluated_at timestamptz not null,
    correlation_id varchar(128) null,
    created_at timestamptz not null default now(),
    constraint uq_attempt_tenant_attempt_id unique (tenant_id, attempt_id),
    constraint fk_attempt_device foreign key (device_id) references devices(id),
    constraint fk_attempt_door foreign key (door_id) references doors(id),
    constraint fk_attempt_pass foreign key (pass_id) references passes(id),
    constraint ck_attempt_decision check (decision in ('GRANTED', 'DENIED'))
);
create index if not exists ix_attempts_tenant_eval on access_attempts (tenant_id, evaluated_at desc);
create index if not exists ix_attempts_tenant_decision on access_attempts (tenant_id, decision);
create index if not exists ix_attempts_tenant_door on access_attempts (tenant_id, door_id);
create index if not exists ix_attempts_tenant_pass on access_attempts (tenant_id, pass_id);