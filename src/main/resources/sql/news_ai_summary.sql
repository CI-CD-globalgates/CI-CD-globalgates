create table tbl_news_ai_summary(
id bigint generated always as identity primary key,
admin_id bigint not null,
post_id bigint not null,
news_summary text not null,
sort_order integer not null,
published_at  timestamp,
created_datetime timestamp not null default now(),
updated_datetime timestamp not null default now(),
constraint fk_summary_admin foreign key(admin_id)
    references tbl_member(id),
constraint fk_summary_post foreign key (post_id)
    references tbl_post(id)
)