create table users (
    id bigserial primary key,
    name varchar(255) not null,
    email varchar(255) not null unique,
    created_at timestamp default now()
);

create table bill_groups (
    id bigserial primary key,
    name varchar(255) not null unique,
    created_by bigint not null references users(id),
    created_at timestamp default now()
);

create table user_group(
    user_id   bigint not null references users (id),
    group_id  bigint not null references bill_groups (id),
    id        bigserial primary key,
    joined_at timestamp default now(),
    constraint user_group_unique unique (group_id, user_id)
);

create index idx_user_group_user_id on user_group (user_id);