-- YeokkeumAI 초기 스키마 (PostgreSQL). Hibernate 생성 DDL과 동일 → ddl-auto=validate 통과.
create table documents (
    id varchar(255) not null,
    filename varchar(255),
    chars integer not null,
    n_chunks integer not null,
    created_at bigint not null,
    primary key (id)
);

create table chunks (
    id varchar(255) not null,
    doc_id varchar(255),
    filename varchar(255),
    idx integer not null,
    text varchar(1000000),
    embedding bytea,
    primary key (id)
);

create table audit_log (
    id varchar(255) not null,
    ts bigint not null,
    actor varchar(255),
    role varchar(255),
    action varchar(255),
    detail varchar(1000000),
    primary key (id)
);
