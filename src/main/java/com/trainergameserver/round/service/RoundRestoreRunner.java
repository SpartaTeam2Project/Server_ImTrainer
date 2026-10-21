package com.trainergameserver.round.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 위반 기록 복구를 서버 안에서만 실행하는 일회성 실행기. HTTP 로는 열려 있지 않습니다.
 * restore.ids 가 있을 때만 켜지고, 처리 후 프로그램을 종료합니다.
 *
 * 운영 EC2 (Session Manager) 에서, 돌고 있는 서버 컨테이너 안에 두 번째 프로세스로 실행:
 *   sudo docker exec imtrainer java -jar app.jar --spring.main.web-application-type=none \
 *        --logging.level.root=WARN --logging.level.com.trainergameserver.round.service=INFO \
 *        --restore.ids=12,15                         # 미리보기
 *   ... --restore.ids=12 --restore.apply=true          # 실제 복구
 *   ... --restore.ids=12 --restore.apply=true --restore.force=true   # 현재 기준으로도 실패하지만 직접 확인한 경우
 */
@Component
@ConditionalOnProperty(name = "restore.ids")
public class RoundRestoreRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoundRestoreRunner.class);

    private final RoundRestoreService roundRestoreService;
    private final Environment env;
    private final ConfigurableApplicationContext context;

    public RoundRestoreRunner(RoundRestoreService roundRestoreService, Environment env,
                              ConfigurableApplicationContext context) {
        this.roundRestoreService = roundRestoreService;
        this.env = env;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean apply = env.getProperty("restore.apply", Boolean.class, false);
        boolean force = env.getProperty("restore.force", Boolean.class, false);
        long[] ids = Arrays.stream(env.getProperty("restore.ids", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).mapToLong(Long::parseLong).toArray();

        log.info("===== 위반 기록 복구 {} (force={}) 대상 {}건 =====", apply ? "실행" : "미리보기", force, ids.length);
        for (long id : ids) {
            try {
                log.info("{}", roundRestoreService.restore(id, apply, force));
            } catch (Exception e) {
                log.warn("#{} 처리 실패, 이 건은 변경 없음(롤백): {}", id, e.toString());
            }
        }
        log.info("===== 끝. 미리보기였다면 --restore.apply=true 로 다시 실행하세요 =====");
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
