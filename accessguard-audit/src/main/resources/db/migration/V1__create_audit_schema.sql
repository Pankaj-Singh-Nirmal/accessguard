-- Audit DB schema (accessguard-audit)
-- Database: accessguard_audit

create table if not exists audit_events (
    event_id uuid primary key,
    tenant_id varchar(64) not null,
    event_type varchar(128) not null,
    schema_version int not null,
    occurred_at timestamptz not null,
    correlation_id varchar(128) null,
    received_at timestamptz not null
);
create index if not exists ix_audit_events_tenant_time on audit_events (tenant_id, occurred_at desc);
create index if not exists ix_audit_events_type on audit_events (event_type);

create table if not exists audit_access_attempts (
    id uuid primary key,
    tenant_id varchar(64) not null,
    event_id uuid not null,
    attempt_id uuid not null,
    device_code varchar(64) null,
    door_code varchar(64) not null,
    zone_code varchar(64) null,
    pass_ref varchar(128) null,
    decision varchar(16) not null,
    reason_code varchar(64) not null,
    occurred_at timestamptz not null,
    evaluated_at timestamptz not null,
    correlation_id varchar(128) null,
    created_at timestamptz not null,
    constraint uq_audit_tenant_attempt unique (tenant_id, attempt_id),
    constraint fk_audit_attempt_event foreign key (event_id) references audit_events(event_id),
    constraint ck_audit_decision check (decision in ('GRANTED', 'DENIED'))
);
create index if not exists ix_audit_attempts_tenant_eval on audit_access_attempts (tenant_id, evaluated_at desc);
create index if not exists ix_audit_attempts_tenant_decision on audit_access_attempts (tenant_id, decision);
create index if not exists ix_audit_attempts_tenant_door on audit_access_attempts (tenant_id, door_code);
create index if not exists ix_audit_attempts_tenant_device on audit_access_attempts (tenant_id, device_code);
create index if not exists ix_audit_attempts_tenant_pass on audit_access_attempts (tenant_id, pass_ref);
