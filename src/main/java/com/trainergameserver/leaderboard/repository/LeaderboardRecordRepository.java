package com.trainergameserver.leaderboard.repository;

import com.trainergameserver.leaderboard.entity.LeaderboardRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardRecordRepository extends JpaRepository<LeaderboardRecord, String> {

    // SELECT * FROM leaderboard ORDER BY kill_count DESC, recorded_at ASC LIMIT ?
    List<LeaderboardRecord> findAllByOrderByKillCountDescRecordedAtAsc(Pageable pageable);
}
