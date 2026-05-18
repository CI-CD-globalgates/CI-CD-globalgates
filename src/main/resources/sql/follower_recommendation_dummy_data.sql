-- ============================================================
-- 추천 커넥션/팔로워 추천 테스트용 더미 데이터
-- 실행 전: tbl_member, tbl_category, tbl_follow, tbl_member_category_rel 이 존재해야 합니다
-- 권장 선행 실행: category_data.sql
-- 비밀번호: 모두 1234 (BCrypt)
-- 대상: 추천 노트북 / 메인 팔로우 추천 / friends 추천 커넥션 테스트
-- 특징:
-- 1. 회원 400명 생성
-- 2. bio 긴 회원 / 짧은 회원 / bio 없는 회원을 섞어서 생성
-- 3. 카테고리 3~4개씩 연결
-- 4. 같은 클러스터 중심 + 일부 교차 팔로우 관계 생성
-- ============================================================

drop table if exists tmp_follower_seed_source;
drop table if exists tmp_follower_seed_members;

create temporary table tmp_follower_seed_source as
with base as (
    select
        gs as seed_no,
        ((gs - 1) / 40) + 1 as cluster_no,
        ((gs - 1) % 40) + 1 as cluster_seq
    from generate_series(1, 400) as gs
)
select
    b.seed_no,
    b.cluster_no,
    b.cluster_seq,
    '추천회원' || lpad(b.seed_no::text, 3, '0') as member_name,
    'followseed' || lpad(b.seed_no::text, 3, '0') || '@globalgates.test' as member_email,
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG' as member_password,
    case b.cluster_no
        when 1 then '미주수출'
        when 2 then '식품수출'
        when 3 then '전자소싱'
        when 4 then '물류운영'
        when 5 then '통관관세'
        when 6 then '무역금융'
        when 7 then '플랫폼IT'
        when 8 then 'K뷰티'
        when 9 then '자동차기계'
        else '섬유원단'
    end || lpad(b.seed_no::text, 3, '0') as member_nickname,
    '@gg_follow_' || lpad(b.seed_no::text, 3, '0') as member_handle,
    '010' || lpad((70000000 + b.seed_no)::text, 8, '0') as member_phone,
    case ((b.seed_no - 1) % 10) + 1
        when 1 then '서울'
        when 2 then '부산'
        when 3 then '인천'
        when 4 then '대구'
        when 5 then '대전'
        when 6 then '광주'
        when 7 then '울산'
        when 8 then '수원'
        when 9 then '창원'
        else '고양'
    end as member_region,
    case
        when b.cluster_seq in (1, 2, 11, 21, 31) or b.seed_no % 5 = 0 then 'expert'
        else 'business'
    end as member_role,
    case
        when b.seed_no % 10 = 0 then null
        when b.seed_no % 10 = 5 then
            case b.cluster_no
                when 1 then '미주 바이어 발굴과 수출 상담을 진행합니다.'
                when 2 then '식품 수출 파트너를 찾고 있습니다.'
                when 3 then '전자부품 소싱과 OEM 연결을 맡고 있습니다.'
                when 4 then '해운·항공 물류 운영을 담당합니다.'
                when 5 then '통관과 HS코드 검토를 지원합니다.'
                when 6 then 'LC와 환율 대응을 돕고 있습니다.'
                when 7 then '무역 플랫폼과 자동화 협업을 합니다.'
                when 8 then '화장품과 건강식품 해외 유통을 담당합니다.'
                when 9 then '자동차부품과 기계장비 거래를 연결합니다.'
                else '섬유·의류 OEM과 원단 소싱을 진행합니다.'
            end
        else
            case b.cluster_no
                when 1 then '미주 시장 바이어 발굴과 FTA 활용 컨설팅을 함께 하는 수출 실무자입니다. 북미 유통 채널, 생활소비재 OEM, 아마존 B2B 납품 경험을 바탕으로 한국 제조사와 해외 바이어를 연결하고 있습니다.'
                when 2 then '동남아와 일본 중심으로 K-Food, 가공식품, 농산물 수출 프로젝트를 운영하고 있습니다. HACCP, 현지 인증, 유통사 상담 경험이 있어 식품 수출 파트너 매칭과 시장 검증을 함께 돕습니다.'
                when 3 then '중국과 일본 공급망에서 전자부품, 완제품, ODM 소싱을 맡고 있습니다. 부품 스펙 협의, MOQ 조정, 납기 관리, 수입 통관까지 연결하는 실무형 네트워크를 운영합니다.'
                when 4 then '부산항 해운, 인천공항 항공, 내륙 운송, 창고 운영을 함께 다루는 물류 담당자입니다. 포워더 협업, 리드타임 단축, 냉장·일반 화물 운영 최적화 경험이 많습니다.'
                when 5 then 'HS코드 분류, FTA 원산지 검토, 수출입 통관, 관세 환급 실무를 지원하고 있습니다. 사전심사와 서류 검토 경험이 많아 초기 무역 기업의 통관 리스크를 줄이는 데 집중합니다.'
                when 6 then '무역금융, LC, 환율, 보험을 함께 보는 실무자입니다. 수출 계약 조건 검토, 신용장 개설, 환리스크 대응, 거래 안정성 확보를 위한 금융 설계를 지원합니다.'
                when 7 then '무역 플랫폼, 자동화, 블록체인 기반 문서 관리에 관심이 많은 IT 실무자입니다. 바이어 매칭, 견적 관리, 무역 문서 디지털화 같은 업무 자동화를 프로젝트 단위로 운영합니다.'
                when 8 then 'K-Beauty와 건강식품 중심으로 해외 유통 파트너를 발굴하고 있습니다. 동남아 인플루언서 마케팅, 미국 이커머스 입점, 샘플 테스트와 리테일 확장 경험을 함께 보유하고 있습니다.'
                when 9 then '자동차부품, 기계장비, 에너지 설비 수출입 프로젝트를 다루고 있습니다. 중동·유럽 플랜트 네트워크, 산업재 견적 협상, 기술 자료 대응 경험을 바탕으로 거래를 연결합니다.'
                else '섬유·의류 OEM, 기능성 원단 소싱, 완제품 수입을 함께 다루고 있습니다. 중국·동남아 생산 파트너, 샘플 개발, 납기 관리, SPA 브랜드 대응 경험을 바탕으로 실무 협업을 진행합니다.'
            end
    end as member_bio,
    case b.cluster_no
        when 1 then '수출'
        when 2 then '식품'
        when 3 then '수입'
        when 4 then '물류'
        when 5 then '관세'
        when 6 then '금융'
        when 7 then 'IT'
        when 8 then '화장품'
        when 9 then '자동차'
        else '섬유/의류'
    end as primary_category,
    case b.cluster_no
        when 1 then case when b.cluster_seq % 2 = 0 then '미주' else '유럽' end
        when 2 then case when b.cluster_seq % 2 = 0 then '동남아' else '일본' end
        when 3 then case when b.cluster_seq % 2 = 0 then '중국' else '전자부품' end
        when 4 then case when b.cluster_seq % 2 = 0 then '해운' else '항공' end
        when 5 then case when b.cluster_seq % 2 = 0 then 'HS코드' else '통관' end
        when 6 then case when b.cluster_seq % 2 = 0 then '무역금융' else '환율' end
        when 7 then case when b.cluster_seq % 2 = 0 then '플랫폼' else '자동화' end
        when 8 then case when b.cluster_seq % 2 = 0 then '건강식품' else '가공식품' end
        when 9 then case when b.cluster_seq % 2 = 0 then '기계/장비' else '중동' end
        else case when b.cluster_seq % 2 = 0 then '원자재' else '완제품' end
    end as secondary_category,
    case b.cluster_no
        when 1 then 'FTA'
        when 2 then '수출'
        when 3 then '일본'
        when 4 then '창고'
        when 5 then '환급'
        when 6 then case when b.cluster_seq % 2 = 0 then '보험' else 'LC' end
        when 7 then '블록체인'
        when 8 then case when b.cluster_seq % 2 = 0 then '동남아' else '미주' end
        when 9 then '에너지'
        else case when b.cluster_seq % 2 = 0 then '중국' else '동남아' end
    end as tertiary_category,
    case b.cluster_no
        when 1 then '무역금융'
        when 2 then 'FTA'
        when 3 then '환율'
        when 4 then '육상'
        when 5 then 'FTA'
        when 6 then '보험'
        when 7 then '플랫폼'
        when 8 then '수출'
        when 9 then '유럽'
        else 'FTA'
    end as quaternary_category
from base b;

insert into tbl_member (
    member_name,
    member_email,
    member_password,
    member_nickname,
    member_handle,
    member_phone,
    member_bio,
    member_region,
    member_role
)
select
    member_name,
    member_email,
    member_password,
    member_nickname,
    member_handle,
    member_phone,
    member_bio,
    member_region,
    member_role::member_role
from tmp_follower_seed_source
on conflict (member_email) do nothing;

create temporary table tmp_follower_seed_members as
select
    m.id,
    s.seed_no,
    s.cluster_no,
    s.cluster_seq,
    s.member_email,
    s.primary_category,
    s.secondary_category,
    s.tertiary_category,
    s.quaternary_category
from tmp_follower_seed_source s
join tbl_member m on m.member_email = s.member_email;

insert into tbl_member_category_rel (member_id, category_id)
select
    seeded.id,
    category_row.id
from tmp_follower_seed_members seeded
join lateral (
    values
        (seeded.primary_category),
        (seeded.secondary_category),
        (seeded.tertiary_category),
        (seeded.quaternary_category)
) as category_name(category_name) on true
join tbl_category category_row
    on category_row.category_name = category_name.category_name
where not exists (
    select 1
    from tbl_member_category_rel rel
    where rel.member_id = seeded.id
      and rel.category_id = category_row.id
);

insert into tbl_follow (follower_id, following_id)
select distinct
    source_member.id,
    target_member.id
from tmp_follower_seed_members source_member
join lateral (
    values
        (((source_member.cluster_no - 1) * 40) + 1),
        (((source_member.cluster_no - 1) * 40) + 2),
        (((source_member.cluster_no - 1) * 40) + (((source_member.cluster_seq + 1 - 1) % 40) + 1)),
        (((source_member.cluster_no - 1) * 40) + (((source_member.cluster_seq + 5 - 1) % 40) + 1)),
        (((source_member.cluster_no - 1) * 40) + (((source_member.cluster_seq + 11 - 1) % 40) + 1)),
        ((((source_member.cluster_no % 10) * 40) + 1))
) as target_seed(seed_no) on true
join tmp_follower_seed_members target_member
    on target_member.seed_no = target_seed.seed_no
where source_member.id <> target_member.id
  and not exists (
      select 1
      from tbl_follow existing_follow
      where existing_follow.follower_id = source_member.id
        and existing_follow.following_id = target_member.id
  );

insert into tbl_follow (follower_id, following_id)
select distinct
    source_member.id,
    target_member.id
from tmp_follower_seed_members source_member
join tmp_follower_seed_members target_member
    on target_member.seed_no = case
        when source_member.cluster_no in (1, 2) then 1
        when source_member.cluster_no in (3, 4) then 81
        when source_member.cluster_no in (5, 6) then 161
        when source_member.cluster_no in (7, 8) then 241
        else 321
    end
where source_member.cluster_seq % 3 = 0
  and source_member.id <> target_member.id
  and not exists (
      select 1
      from tbl_follow existing_follow
      where existing_follow.follower_id = source_member.id
        and existing_follow.following_id = target_member.id
  );

-- 실행 결과 확인용
select count(*) as seeded_member_count
from tbl_member
where member_email like 'followseed%@globalgates.test';

select count(*) as seeded_member_category_rel_count
from tbl_member_category_rel
where member_id in (
    select id
    from tbl_member
    where member_email like 'followseed%@globalgates.test'
);

select count(*) as seeded_follow_count
from tbl_follow
where follower_id in (
    select id
    from tbl_member
    where member_email like 'followseed%@globalgates.test'
)
or following_id in (
    select id
    from tbl_member
    where member_email like 'followseed%@globalgates.test'
);
