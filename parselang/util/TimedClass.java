package parselang.util;

import java.util.HashSet;
import java.util.Set;

public abstract class TimedClass {

    private final Set<Long> measurements = new HashSet<>();
    private long startTime;

    protected void start() {
        startTime = System.nanoTime();
    }

    protected void stop() {
        measurements.add(System.nanoTime() - startTime);
    }

    public double getTotalTime() {
        return measurements.stream().reduce(0L, Long::sum).doubleValue() / 1000000000.0;
    }

}
