package com.trainergameserver.leaderboard.service;

import com.trainergameserver.leaderboard.dto.LeaderboardEntry;
import com.trainergameserver.leaderboard.dto.LeaderboardResponse;
import com.trainergameserver.leaderboard.entity.LeaderboardRecord;
import com.trainergameserver.leaderboard.repository.LeaderboardRecordRepository;
import com.trainergameserver.round.dto.RoundEndRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class LeaderboardService {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final LeaderboardRecordRepository leaderboardRecordRepository;

    public LeaderboardService(LeaderboardRecordRepository leaderboardRecordRepository) {
        this.leaderboardRecordRepository = leaderboardRecordRepository;
    }

    /** 트레이너의 최고 기록 갱신 (없으면 새로 생성) */
    @Transactional
    public void record(String username, RoundEndRequest req, LocalDateTime now) {
        leaderboardRecordRepository.findById(username)
                .ifPresentOrElse(
                        existing -> existing.updateIfBetter(req, now),
                        () -> leaderboardRecordRepository.save(LeaderboardRecord.of(username, req, now))
                );
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getTop(int size) {
        int safeSize = Math.max(MIN_SIZE, Math.min(size, MAX_SIZE));

        List<LeaderboardRecord> records =
                leaderboardRecordRepository.findAllByOrderByKillCountDescRecordedAtAsc(PageRequest.of(0, safeSize));

        List<LeaderboardEntry> entries = IntStream.range(0, records.size())
                .mapToObj(i -> LeaderboardEntry.of(i + 1, records.get(i)))
                .toList();

        return new LeaderboardResponse(entries);
    }
}
