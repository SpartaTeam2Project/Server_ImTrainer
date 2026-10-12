package com.trainergameserver.service;

import com.trainergameserver.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 만료된 리프레시 토큰을 매일 새벽 정리합니다. 서버가 여러 대여도 같은 DELETE라 중복 실행돼도 안전합니다. */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "${jwt.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpired(LocalDateTime.now());
        log.info("만료된 리프레시 토큰 {}건 삭제", deleted);
    }
}
