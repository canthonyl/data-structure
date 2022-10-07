package canthonyl.fixture;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

public class FixedClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final AtomicReference<ZoneId> zoneId;

    public FixedClock(ZoneId zoneId, Instant instant){
        this.zoneId = new AtomicReference<>(zoneId);
        this.instant = new AtomicReference<>(instant);
    }

    public FixedClock(){
        this(ZoneId.systemDefault(), Instant.now());
    }

    public FixedClock(LocalTime time){
        this(ZoneId.systemDefault(), Instant.now());
        this.setTime(time);
    }

    public FixedClock(long epochMilli){
        this(ZoneId.of("UTC"), Instant.ofEpochMilli(epochMilli));
    }

    @Override
    public ZoneId getZone() {
        return zoneId.get();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        zoneId.set(zone);
        return this;
    }

    @Override
    public Instant instant() {
        return instant.get();
    }

    public void setTime(LocalTime time){
        ZoneId zid = getZone();
        instant.getAndUpdate(i -> ZonedDateTime.of(i.atZone(zid).toLocalDate(), time, zid).toInstant());
    }

    public LocalTime getLocalTime(){
        return instant.get().atZone(getZone()).toLocalTime();
    }

    public LocalDateTime getLocalDateTime() {return instant.get().atZone(getZone()).toLocalDateTime(); }

    public void advanceMillis(long millisToAdvance){
        instant.getAndUpdate(i -> i.plusMillis(millisToAdvance));
    }

    @Override
    public long millis() {
        return instant.get().toEpochMilli();
    }
}