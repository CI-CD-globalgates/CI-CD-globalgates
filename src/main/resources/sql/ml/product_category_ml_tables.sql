-- Product category recommendation ML tables
-- Purpose:
-- 1) keep human-confirmed labels separate from live product data
-- 2) snapshot training datasets for reproducible experiments
-- 3) log model predictions and user feedback independently

create table if not exists tbl_product_category_label (
    id                    bigint generated always as identity primary key,
    product_post_id       bigint not null,
    labeled_category_id   bigint not null,
    labeling_source       varchar(30) not null,
    label_status          varchar(20) not null default 'confirmed',
    confidence_note       varchar(255),
    labeled_by_member_id  bigint,
    created_datetime      timestamp not null default now(),
    updated_datetime      timestamp not null default now(),

    constraint fk_pc_label_product
        foreign key (product_post_id) references tbl_post_product(id),
    constraint fk_pc_label_category
        foreign key (labeled_category_id) references tbl_category(id),
    constraint fk_pc_label_member
        foreign key (labeled_by_member_id) references tbl_member(id),
    constraint uk_pc_label_product
        unique (product_post_id)
);

create index if not exists idx_pc_label_category
    on tbl_product_category_label(labeled_category_id);

create index if not exists idx_pc_label_source_status
    on tbl_product_category_label(labeling_source, label_status);


create table if not exists tbl_product_category_training_snapshot (
    id                       bigint generated always as identity primary key,
    snapshot_name            varchar(100) not null unique,
    snapshot_version         integer not null,
    total_samples            integer not null default 0,
    train_samples            integer not null default 0,
    valid_samples            integer not null default 0,
    test_samples             integer not null default 0,
    category_schema_version  integer not null default 1,
    feature_note             text,
    built_by_member_id       bigint,
    created_datetime         timestamp not null default now(),

    constraint fk_pc_snapshot_member
        foreign key (built_by_member_id) references tbl_member(id)
);


create table if not exists tbl_product_category_training_sample (
    id                           bigint generated always as identity primary key,
    training_snapshot_id         bigint not null,
    product_post_id              bigint not null,
    post_title_snapshot          varchar(255),
    post_content_snapshot        text,
    post_tag_snapshot            varchar(1000),
    product_price_snapshot       integer,
    category_id_snapshot         bigint not null,
    parent_category_id_snapshot  bigint,
    split_type                   varchar(10) not null,
    created_datetime             timestamp not null default now(),

    constraint fk_pc_sample_snapshot
        foreign key (training_snapshot_id) references tbl_product_category_training_snapshot(id),
    constraint fk_pc_sample_product
        foreign key (product_post_id) references tbl_post_product(id),
    constraint fk_pc_sample_category
        foreign key (category_id_snapshot) references tbl_category(id),
    constraint fk_pc_sample_parent_category
        foreign key (parent_category_id_snapshot) references tbl_category(id)
);

create index if not exists idx_pc_sample_snapshot_split
    on tbl_product_category_training_sample(training_snapshot_id, split_type);

create index if not exists idx_pc_sample_product
    on tbl_product_category_training_sample(product_post_id);


create table if not exists tbl_product_category_prediction (
    id                          bigint generated always as identity primary key,
    product_post_id             bigint not null,
    model_name                  varchar(100) not null,
    model_version               varchar(50) not null,
    predicted_category_id_top1  bigint not null,
    predicted_category_id_top2  bigint,
    predicted_category_id_top3  bigint,
    score_top1                  numeric(5,4) not null,
    score_top2                  numeric(5,4),
    score_top3                  numeric(5,4),
    prediction_source           varchar(30) not null,
    request_context             varchar(30) not null,
    is_applied                  boolean not null default false,
    created_datetime            timestamp not null default now(),

    constraint fk_pc_pred_product
        foreign key (product_post_id) references tbl_post_product(id),
    constraint fk_pc_pred_cat1
        foreign key (predicted_category_id_top1) references tbl_category(id),
    constraint fk_pc_pred_cat2
        foreign key (predicted_category_id_top2) references tbl_category(id),
    constraint fk_pc_pred_cat3
        foreign key (predicted_category_id_top3) references tbl_category(id)
);

create index if not exists idx_pc_pred_product_created
    on tbl_product_category_prediction(product_post_id, created_datetime desc);

create index if not exists idx_pc_pred_model_version
    on tbl_product_category_prediction(model_name, model_version);


create table if not exists tbl_product_category_feedback (
    id                    bigint generated always as identity primary key,
    product_post_id       bigint not null,
    prediction_id         bigint,
    selected_category_id  bigint not null,
    was_ai_recommended    boolean not null default false,
    was_top1_selected     boolean not null default false,
    was_top3_selected     boolean not null default false,
    feedback_type         varchar(30) not null,
    feedback_note         varchar(255),
    actor_member_id       bigint,
    created_datetime      timestamp not null default now(),

    constraint fk_pc_feedback_product
        foreign key (product_post_id) references tbl_post_product(id),
    constraint fk_pc_feedback_prediction
        foreign key (prediction_id) references tbl_product_category_prediction(id),
    constraint fk_pc_feedback_category
        foreign key (selected_category_id) references tbl_category(id),
    constraint fk_pc_feedback_member
        foreign key (actor_member_id) references tbl_member(id)
);

create index if not exists idx_pc_feedback_product_created
    on tbl_product_category_feedback(product_post_id, created_datetime desc);

create index if not exists idx_pc_feedback_selected_category
    on tbl_product_category_feedback(selected_category_id);
