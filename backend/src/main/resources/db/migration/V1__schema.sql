-- Schema for the nutrition label trial-calculation app.
-- Portable SQL (runs on PostgreSQL and on H2 for tests).

create table rule_set (
    id         bigint       primary key,
    code       varchar(64)  not null unique,
    name       varchar(255) not null,
    disclaimer varchar(1000) not null
);

create table rule_version (
    id          bigint       primary key,
    rule_set_id bigint       not null references rule_set (id),
    version     varchar(32)  not null,
    note        varchar(1000),
    unique (rule_set_id, version)
);

create table nutrient (
    id                 bigint       primary key,
    code               varchar(64)  not null unique,
    name               varchar(255) not null,
    storage_unit       varchar(16)  not null,
    display_order      int          not null,
    derived_from_code  varchar(64),
    derive_multiplier  numeric(19, 6),
    derive_divisor     numeric(19, 6)
);

create table nutrient_rule (
    id                 bigint         primary key,
    rule_version_id    bigint         not null references rule_version (id),
    nutrient_id        bigint         not null references nutrient (id),
    display_unit       varchar(16)    not null,
    rounding_increment numeric(19, 6) not null,
    zero_threshold     numeric(19, 6) not null,
    trace_threshold    numeric(19, 6),
    unique (rule_version_id, nutrient_id)
);

create table ingredient (
    id   bigint       primary key,
    name varchar(255) not null,
    note varchar(1000)
);

create table ingredient_nutrient (
    id              bigint        primary key,
    ingredient_id   bigint        not null references ingredient (id),
    nutrient_id     bigint        not null references nutrient (id),
    value_per_100g  numeric(19, 6),  -- null = unknown (data gap, never zero)
    unique (ingredient_id, nutrient_id)
);

create table recipe (
    id                      bigint         primary key,
    name                    varchar(255)   not null,
    description             varchar(1000),
    default_serving_size_g  numeric(19, 6) not null,
    yield_weight_g          numeric(19, 6)  -- null -> use sum of ingredient amounts
);

create table recipe_ingredient (
    id            bigint         primary key,
    recipe_id     bigint         not null references recipe (id),
    ingredient_id bigint         not null references ingredient (id),
    amount_g      numeric(19, 6) not null
);
