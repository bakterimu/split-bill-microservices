create table settlements (
    id           bigserial     primary key,
    group_id     bigint        not null,
    from_user_id bigint        not null,
    to_user_id   bigint        not null,
    amount       numeric(15,2) not null check (amount > 0),
    note         varchar(255),
    paid_at      timestamp     not null default now(),
    constraint settlements_different_users check (from_user_id <> to_user_id)
);

create index idx_settlements_group_paid_at on settlements (group_id, paid_at);
