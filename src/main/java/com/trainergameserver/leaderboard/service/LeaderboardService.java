package com.trainergameserver.leaderboard.service;

import com.trainergameserver.leaderboard.dto.LeaderboardEntry;
import com.trainergameserver.leaderboard.dto.LeaderboardResponse;
import com.trainergameserver.leaderboard.dto.LeaderboardRow;
import com.trainergameserver.leaderboard.dto.MyRankResponse;
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
        List<LeaderboardRow> rows = leaderboardRecordRepository.findRanking(PageRequest.of(0, safeSize));

        List<LeaderboardEntry> entries = IntStream.range(0, rows.size())
                .mapToObj(i -> toEntry(i + 1, rows.get(i)))
                .toList();
        return new LeaderboardResponse(entries);
    }

    /** 내 순위. 상위 목록 밖이어도 몇 등인지 계산하고, 기록이 없으면 ranked=false */
    @Transactional(readOnly = true)
    public MyRankResponse getMyRank(String username) {
        LeaderboardEntry entry = leaderboardRecordRepository.findRow(username)
                .map(row -> {
                    LeaderboardRecord r = row.record();
                    int rank = (int) leaderboardRecordRepository.countAhead(r.getKillCount(), r.getRecordedAt()) + 1;
                    return toEntry(rank, row);
                })
                .orElse(null);
        return MyRankResponse.of(entry);
    }

    /**
     * 리더보드에는 검증을 통과한 기록만 올라오고, 검증은 보유 포켓몬이 있어야(= 인트로 완료) 통과하므로
     * 닉네임은 항상 인트로에서 정한 이름입니다. 엔트리 검증 규칙을 바꿀 때 이 전제가 유지되는지 확인하세요.
     */
    private LeaderboardEntry toEntry(int rank, LeaderboardRow row) {
        LeaderboardRecord r = row.record();
        return new LeaderboardEntry(rank, row.nickname(), r.getLevel(),r.getKillCount(), r.pokemonEntryIds());
    }
}