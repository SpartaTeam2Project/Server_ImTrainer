package com.trainergameserver.leaderboard.repository;

import com.trainergameserver.leaderboard.dto.LeaderboardRow;
import com.trainergameserver.leaderboard.entity.LeaderboardRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeaderboardRecordRepository extends JpaRepository<LeaderboardRecord, String> {

    /**
     * 랭킹 순서(처치 수 DESC, 달성 시각 ASC)로 기록과 닉네임을 함께 읽습니다.
     * leaderboard.trainer_id(username) → users → players(user_id) 로 이어 붙입니다.
     */
    @Query("""
            select new com.trainergameserver.leaderboard.dto.LeaderboardRow(l, p.nickname, p.introDone)
            from LeaderboardRecord l
            join User u on u.username = l.trainerId
            left join Player p on p.userId = u.id
            order by l.killCount desc, l.recordedAt asc
            """)
    List<LeaderboardRow> findRanking(Pageable pageable);
}
