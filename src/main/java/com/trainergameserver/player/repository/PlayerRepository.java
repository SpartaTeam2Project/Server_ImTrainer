package com.trainergameserver.player.repository;

import com.trainergameserver.player.entity.Player;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * 플레이데이터 행이 없으면 신규 계정 기본값으로 만듭니다. 이미 있으면 아무것도 하지 않습니다.
     * 같은 계정으로 동시에 첫 요청이 와도 PK 충돌 없이 한 행만 생깁니다.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO players (user_id, nickname, intro_done, intro_gender, intro_generation,
                                 starter_monster_id, selected_trainer_id, created_at, updated_at)
            VALUES (:userId, :nickname, 0, 0, 0, 0, 0, NOW(6), NOW(6))
            ON DUPLICATE KEY UPDATE user_id = user_id
            """)
    int createIfAbsent(@Param("userId") Long userId, @Param("nickname") String nickname);

    /** 쓰기 요청용. 같은 계정의 요청이 동시에 와도 하나씩 처리되도록 행을 잠급니다(SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.userId = :userId")
    Optional<Player> findForUpdate(@Param("userId") Long userId);
}
