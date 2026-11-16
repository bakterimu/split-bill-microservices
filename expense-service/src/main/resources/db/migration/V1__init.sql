create table expenses (
    id           bigserial     primary key,
    group_id     bigint        not null,
    paid_by      bigint        not null,
    description  varchar(255)  not null,
    category     varchar(30)   not null,
    amount       numeric(15,2) not null check (amount > 0),
    split_type   varchar(20)   not null,
    expense_date date          not null,
    created_at   timestamp     not null default now()
);

create table expense_shares (
    id           bigserial     primary key,
    expense_id   bigint        not null references expenses (id) on delete cascade,
    user_id      bigint        not null,
    share_amount numeric(15,2) not null check (share_amount >= 0),
    constraint expense_shares_unique unique (expense_id, user_id)
);

create index idx_expenses_group_date on expenses (group_id, expense_date);
create index idx_expense_shares_user on expense_shares (user_id);
