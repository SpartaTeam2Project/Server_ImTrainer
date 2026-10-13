package com.trainergameserver.master.service;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 마스터데이터 테이블 하나. resources/masterdata/{name}.json 파일이 원본이고 DB는 파일 내용을 따라갑니다.
 * <p>
 * MasterDataService가 서버 시작 시 다음 순서로 호출합니다.
 * <ol>
 *   <li>{@link #validate}: 파일 값 검사 (빈 값, 소수 자릿수 등). 실패하면 서버가 뜨지 않습니다.</li>
 *   <li>{@link #sync}: 파일 내용을 DB에 반영 (트랜잭션 안)</li>
 *   <li>{@link #load}: DB에서 다시 읽어 클라에 내려줄 형태로 만듦 (트랜잭션 안)</li>
 * </ol>
 * load 결과는 {@link #current()}로 다른 서비스(구매 검증 등)에서 읽을 수 있습니다.
 * 새 테이블은 이 클래스를 상속한 @Component를 만들고, 엔티티·리포지토리·Flyway 마이그레이션을 추가하면 됩니다.
 *
 * @param <T> 파일과 응답의 JSON 구조 (같은 형태)
 */
public abstract class MasterTable<T> {

    /** DB 컬럼이 DECIMAL(10,3)이라 소수점 셋째 자리까지만 저장됩니다. 넘으면 조용히 반올림되지 않도록 시작 시 실패시킵니다. */
    protected static final int MAX_SCALE = 3;

    private volatile T current;

    /** 테이블 이름. 클라 요청/응답의 key이자 파일 이름(masterdata/{name}.json)입니다. */
    public abstract String name();

    public abstract Class<T> type();

    protected abstract void validate(T file);

    protected abstract void sync(T file);

    protected abstract T load();

    public String filePath() {
        return "masterdata/" + name() + ".json";
    }

    /** DB에서 읽어 클라에 내려준 것과 같은 데이터. 서버 시작이 끝난 뒤에만 사용합니다. */
    public T current() {
        T value = current;
        if (value == null) {
            throw new IllegalStateException("마스터데이터 " + name() + "가 아직 준비되지 않았습니다.");
        }
        return value;
    }

    void setCurrent(T value) {
        this.current = value;
    }

    /** 값 중 하나라도 null이면 서버 시작 실패 */
    protected void requireValues(Object row, Object... values) {
        for (Object value : values) {
            if (value == null) {
                throw new IllegalStateException(filePath() + ": 비어 있는 값이 있습니다. " + row);
            }
        }
    }

    /** 소수점 셋째 자리를 넘는 값이 있으면 서버 시작 실패 */
    protected void requireScale(Object row, BigDecimal... values) {
        requireValues(row, (Object[]) values);
        for (BigDecimal value : values) {
            if (value.stripTrailingZeros().scale() > MAX_SCALE) {
                throw new IllegalStateException(filePath() + ": 소수점 " + MAX_SCALE + "자리를 넘는 값이 있습니다. " + row);
            }
        }
    }

    protected IllegalStateException duplicateId(Object id) {
        return new IllegalStateException(filePath() + ": id가 중복됩니다. id=" + id);
    }

    /** DB DECIMAL(10,3)에서 읽은 0.700, 6.000을 0.7, 6으로 (같은 데이터는 항상 같은 문자열이 되도록) */
    protected static BigDecimal plain(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    /** 소수 자릿수(0.7 vs 0.700)와 관계없이 값이 같은지 */
    protected static boolean sameNumber(BigDecimal a, BigDecimal b) {
        return Objects.equals(a, b) || (a != null && b != null && a.compareTo(b) == 0);
    }
}
