package com.trainergameserver.leaderboard.repository;

import com.trainergameserver.leaderboard.dto.LeaderboardRow;
import com.trainergameserver.leaderboard.entity.LeaderboardRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /** 한 트레이너의 기록 + 닉네임 (내 순위 표시용) */
    @Query("""
            select new com.trainergameserver.leaderboard.dto.LeaderboardRow(l, p.nickname, p.introDone)
            from LeaderboardRecord l
            join User u on u.username = l.trainerId
            left join Player p on p.userId = u.id
            where l.trainerId = :trainerId
            """)
    Optional<LeaderboardRow> findRow(@Param("trainerId") String trainerId);

    /** 이 기록보다 앞선 기록 수 (처치 수가 많거나, 같으면 먼저 달성). 순위 = 이 값 + 1 */
    @Query("""
            select count(l) from LeaderboardRecord l
            where l.killCount > :killCount
               or (l.killCount = :killCount and l.recordedAt < :recordedAt)
            """)
    long countAhead(@Param("killCount") int killCount, @Param("recordedAt") LocalDateTime recordedAt);
}
