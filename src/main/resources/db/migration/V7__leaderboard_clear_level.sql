-- 리더보드를 클리어한 판만 기록하도록 변경하면서 클리어 레벨 추가.
-- 기존 기록에는 실패(DEAD) 판이 섞여 있고 구분할 컬럼이 없어 비웁니다 (테스트 데이터만 있는 시점).
DELETE FROM leaderboard;
ALTER TABLE leaderboard ADD COLUMN level INT NOT NULL;
