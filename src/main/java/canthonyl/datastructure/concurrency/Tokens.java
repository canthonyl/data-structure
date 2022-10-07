package canthonyl.datastructure.concurrency;

import canthonyl.datastructure.concurrency.lock.AtomicBooleanLock;
import canthonyl.datastructure.concurrency.lock.OptimisticLock;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

public class Tokens {

    private final Clock clock;
    private final Long totalNumTokens;
    private final OptimisticLock lock;
    private final TokenLevel[] levels;
    private final Long windowMilli;
    private final Long unit;
    private final TimeUnit timeUnit;
    private final Long[] tokensAtLevel;
    private Long clockOffsetMillis;

    public Tokens(Clock clock, Long totalNumTokens, Long unit, TimeUnit timeUnit) {
        this.clock = clock;
        this.windowMilli = timeUnit.convert(unit, TimeUnit.MILLISECONDS);
        this.lock = new AtomicBooleanLock();
        this.totalNumTokens = totalNumTokens;
        this.tokensAtLevel = new Long[]{totalNumTokens};
        this.levels = init(tokensAtLevel);
        this.unit = unit;
        this.timeUnit = timeUnit;
        this.clockOffsetMillis = 0L;
    }

    public Tokens(Clock clock, Long unit, TimeUnit timeUnit, Long... tokensAtLevel) {
        this.clock = clock;
        this.windowMilli = timeUnit.convert(unit, TimeUnit.MILLISECONDS);
        this.lock = new AtomicBooleanLock();
        this.totalNumTokens = Arrays.stream(tokensAtLevel).reduce((a,b) -> a+b).get();
        this.tokensAtLevel = tokensAtLevel;
        this.levels = init(tokensAtLevel);
        this.unit = unit;
        this.timeUnit = timeUnit;
        this.clockOffsetMillis = 0L;
    }

    public Long getTotalNumTokens() {
        return totalNumTokens;
    }

    public Long getWindowMilli() {
        return windowMilli;
    }

    public Tokens withOffset(Long millis) {
        Tokens tokens = new Tokens(Clock.offset(clock, Duration.ofMillis(millis)), unit, timeUnit, tokensAtLevel);
        tokens.clockOffsetMillis = millis;
        return tokens;
    }

    private TokenLevel[] init(Long[] tokensAtLevel) {
        Long now = clock.millis();
        TokenLevel[] arr = new TokenLevel[tokensAtLevel.length];
        for (int i=0; i<arr.length; i++) {
            arr[i] = new TokenLevel(tokensAtLevel[i], now);
        }
        return arr;
    }

    public Long nextAvailableTimestamp(Long levelIndex){
        lock.acquire();
        try {
            Long timestamp = Long.MAX_VALUE;
            for (Long i = levelIndex; i < levels.length; i++) {
                timestamp = Math.min(timestamp, levels[i.intValue()].peek());
            }
            return timestamp;
        } finally {
            lock.release();
        }
    }

    public Long nextAvailableTimestamp(){
        return nextAvailableTimestamp(0L);
    }

    public Long reserve(Long levelIndex) {
        lock.acquire();
        try {
            Long now = clock.millis();
            Optional<TokenLevel> reserveLevel = Optional.empty();

            for (Long i=levelIndex; i<levels.length; i++) {
                TokenLevel level = levels[i.intValue()];
                int next = (level.index + 1) % level.timestamps.length;
                AtomicLong nextSlot = level.timestamps[next];
                Long nextAvailableTimestamp = nextSlot.get() + windowMilli;
                if (nextAvailableTimestamp <= now) {
                    nextSlot.set(now);
                    level.timestampsWithOffset[next].set(now + windowMilli);
                    level.index = next;
                    level.reserveTaken = 0L;
                    return 0L;
                }
                if (!reserveLevel.isPresent()) {
                    reserveLevel = Optional.of(level);
                } else {
                    if (level.peekReserve() < reserveLevel.get().peekReserve()) {
                        reserveLevel = Optional.of(level);
                    }
                }
            }

            return reserveLevel.get().takeReserve();
        } finally {
            lock.release();
        }
    }

    public Long reserve() {
        return reserve(0L);
    }

    public Long reserveAll(Long millisApart) {
        lock.acquire();
        try {
            Long tsToSet = clock.millis();
            for (TokenLevel level : levels) {
                level.reserveTaken = 0L;
                for (Long i=0L; i<levels.length; i++) {
                    level.timestamps[i.intValue()].set(tsToSet + millisApart);
                    level.timestampsWithOffset[i.intValue()].set(tsToSet + millisApart + windowMilli);
                }
            }
            return 0L;
        } finally {
            lock.release();
        }
    }



    public boolean get(Long level) {
        lock.acquire();
        try {
            TokenLevel tLevel = levels[level.intValue()];
            int next = (tLevel.index + 1) % tLevel.timestamps.length;
            AtomicLong nextSlot = tLevel.timestamps[next];
            long now = clock.millis();
            boolean expired = ( now - nextSlot.get() ) >= windowMilli;
            if (expired) {
                nextSlot.set(now);
                tLevel.index = next;
            }
            return expired;
        } finally {
            lock.release();
        }
    }

    public boolean get() {
        return get(0L);
    }

    public Long availableTokensAfter(Long levelIndex, Long timestampInclusive) {
        lock.acquire();
        try {
            Long count = 0L;
            for (Long i=levelIndex; i<levels.length; i++) {
                count += levels[i.intValue()].availableTokensAfter(timestampInclusive);
            }
            return count;
        } finally {
            lock.release();
        }
    }

    public Long availableTokensAfter(Long timestampInclusive) {
        return availableTokensAfter(0L, timestampInclusive);
    }

    private class TokenLevel {
        private final Long levelNumTokens;
        private final AtomicLong[] timestamps;
        private final AtomicLong[] timestampsWithOffset;
        private volatile int index;
        private volatile Long reserveTaken;
        
        private TokenLevel(Long levelNumTokens, Long now) {
            this.levelNumTokens = levelNumTokens;
            this.reserveTaken = 0L;
            this.timestamps = LongStream.range(0, levelNumTokens).mapToObj(i -> new AtomicLong(now - windowMilli)).toArray(AtomicLong[]::new);
            this.timestampsWithOffset = LongStream.range(0, levelNumTokens).mapToObj(i -> new AtomicLong(now)).toArray(AtomicLong[]::new);
        }

        private Long availableTokensAfter(Long timestampInclusive) {
            if (timestamps[index].get() + windowMilli <= timestampInclusive) {
                return levelNumTokens;
            } else {
                int i = (index + 1) % timestamps.length;
                Long count = 0L;
                while (timestamps[i].get() + windowMilli <= timestampInclusive) {
                    count++;
                    i = (i + 1) % timestamps.length;
                }
                return count;
            }
        }

        private Long peek(){
            int next = (index + 1) % timestamps.length;
            AtomicLong nextSlot = timestamps[next];
            return nextSlot.get() + windowMilli;
        }

        private Long peekReserve() {
            Long offsetIndex = (index + Math.min(reserveTaken + 1, levelNumTokens)) % timestampsWithOffset.length;
            return timestampsWithOffset[offsetIndex.intValue()].get();
        }

        private Long takeReserve() {
            Long reserveTimestamp = peekReserve();
            if (reserveTaken < levelNumTokens) {
                reserveTaken++;
            }
            return reserveTimestamp;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tokens tokens = (Tokens) o;
        return Objects.equals(totalNumTokens, tokens.totalNumTokens) && Objects.equals(windowMilli, tokens.windowMilli);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalNumTokens, windowMilli);
    }

    @Override
    public String toString() {
        return "Tokens{" +
                "totalNumTokens=" + totalNumTokens +
                ", windowMilli=" + windowMilli +
                ", clockOffsetMillis=" + clockOffsetMillis +
                '}';
    }
}
