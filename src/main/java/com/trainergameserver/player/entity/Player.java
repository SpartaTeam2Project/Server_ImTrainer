package com.trainergameserver.player.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 계정별 플레이데이터. users.id와 같은 id를 씁니다.
 * 행은 처음 /player/* 를 호출할 때 PlayerRepository.createIfAbsent로 만들어집니다.
 */
@Entity
@Table(name = "players")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {

    @Id
    private Long userId;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false)
    private Boolean introDone;

    /** 인트로에서 고른 성별 (0 남자, 1 여자). 인트로 전이면 0 */
    @Column(nullable = false)
    private Integer introGender;

    /** 인트로에서 고른 세대. 인트로 전이면 0 */
    @Column(nullable = false)
    private Integer introGeneration;

    /** 인트로에서 고른 스타터 포켓몬 id. 인트로 전이면 0 */
    @Column(nullable = false)
    private Integer starterMonsterId;

    /** 현재 고른 트레이너. 없으면 0 */
    @Column(nullable = false)
    private Integer selectedTrainerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 트레이닝 id → 산 레벨 수. 0레벨(미구매)은 저장하지 않습니다. */
    @ElementCollection
    @CollectionTable(name = "player_trainings", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "training_id")
    @Column(name = "level", nullable = false)
    private Map<Integer, Integer> trainings = new HashMap<>();

    /** 재화 id → 잔액 */
    @ElementCollection
    @CollectionTable(name = "player_currencies", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "currency_id", length = 30)
    @Column(name = "amount", nullable = false)
    private Map<String, Long> currencies = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "player_trainers", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "trainer_id", nullable = false)
    private Set<Integer> ownedTrainerIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "player_monsters", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "monster_id", nullable = false)
    private Set<Integer> ownedMonsterIds = new HashSet<>();

    public int trainingLevel(int trainingId) {
        return trainings.getOrDefault(trainingId, 0);
    }

    public long balance(String currencyId) {
        return currencies.getOrDefault(currencyId, 0L);
    }

    public void completeIntro(String nickname, int gender, int generation, int starterMonsterId,
                              Iterable<Integer> grantedTrainerIds, int selectedTrainerId) {
        this.nickname = nickname;
        this.introDone = true;
        this.introGender = gender;
        this.introGeneration = generation;
        this.starterMonsterId = starterMonsterId;
        grantedTrainerIds.forEach(ownedTrainerIds::add);
        this.ownedMonsterIds.add(starterMonsterId);
        this.selectedTrainerId = selectedTrainerId;
        touch();
    }

    public void setTrainingLevel(int trainingId, int level) {
        if (level <= 0) {
            trainings.remove(trainingId);
        } else {
            trainings.put(trainingId, level);
        }
        touch();
    }

    public void resetTrainings() {
        trainings.clear();
        touch();
    }

    public void setBalance(String currencyId, long amount) {
        currencies.put(currencyId, amount);
        touch();
    }

    public void selectTrainer(int trainerId) {
        this.selectedTrainerId = trainerId;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
