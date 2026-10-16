package com.trainergameserver.round.repository;

import com.trainergameserver.round.entity.RoundViolation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundViolationRepository extends JpaRepository<RoundViolation, Long> {

    /** 계정별 위반 횟수 (나중에 제재 기준을 정할 때 사용) */
    long countByUserId(Long userId);
}
