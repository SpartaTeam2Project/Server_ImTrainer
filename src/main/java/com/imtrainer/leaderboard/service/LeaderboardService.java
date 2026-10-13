package com.imtrainer.leaderboard.service;

import com.imtrainer.leaderboard.dto.LeaderboardEntry;
import com.imtrainer.leaderboard.dto.LeaderboardResponse;
import com.imtrainer.leaderboard.entity.LeaderboardRecord;
import com.imtrainer.leaderboard.repository.LeaderboardRecordRepository;
import com.imtrainer.round.dto.RoundEndRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LeaderboardService {
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final LeaderboardRecordRepository leaderboardRecordRepository;

    @Transactional
    public void record(RoundEndRequest req, LocalDateTime now) {
        leaderboardRecordRepository.findById(req.trainerId()).ifPresentOrElse(existing -> existing.updateIfBetter(req, now),
                () -> leaderboardRecordRepository.save(LeaderboardRecord.from(req, now)));
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
