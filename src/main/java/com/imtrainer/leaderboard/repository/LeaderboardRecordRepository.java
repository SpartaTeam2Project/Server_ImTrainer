package com.imtrainer.leaderboard.repository;

import com.imtrainer.leaderboard.entity.LeaderboardRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardRecordRepository extends JpaRepository<LeaderboardRecord, String> {
    List<LeaderboardRecord> findAllByOrderByKillCountDescRecordedAtAsc(Pageable pageable);
}
