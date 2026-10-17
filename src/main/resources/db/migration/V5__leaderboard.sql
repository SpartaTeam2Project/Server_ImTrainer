-- 리더보드: 트레이너별 최고 기록 1건. trainer_id = users.username
-- 랭킹 기준: kill_count 내림차순, 같으면 recorded_at 오름차순 (먼저 달성한 사람이 위)
CREATE TABLE leaderboard (
                             trainer_id    VARCHAR(20) NOT NULL,
                             kill_count    INT         NOT NULL,
                             pokemon_entry VARCHAR(50) NOT NULL,
                             recorded_at   DATETIME(6) NOT NULL,
                             PRIMARY KEY (trainer_id),
                             INDEX idx_leaderboard_rank (kill_count, recorded_at)
);