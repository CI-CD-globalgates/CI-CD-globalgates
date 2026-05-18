create table tbl_message_translation (
 id                      bigint    generated always as identity primary key,
 message_id              bigint    not null,
 target_language         varchar(16) not null,
 source_updated_datetime timestamp not null,
 translated_text         text      not null,
 created_datetime        timestamp not null default now(),
 updated_datetime        timestamp not null default now(),

 constraint uq_message_translation unique (message_id, target_language),
 constraint fk_message_translation_message
     foreign key (message_id) references tbl_message(id)
);

create table tbl_post_translation (
  id                      bigint    generated always as identity primary key,
  post_id                 bigint    not null,
  target_language         varchar(16) not null,
  source_updated_datetime timestamp not null,
  translated_text         text      not null,
  created_datetime        timestamp not null default now(),
  updated_datetime        timestamp not null default now(),

  constraint uq_post_translation unique (post_id, target_language),
  constraint fk_post_translation_post
      foreign key (post_id) references tbl_post(id)
);