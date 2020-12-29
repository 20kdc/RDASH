package kdc.sync2.core;

public final class IndexTime implements Comparable<IndexTime> {
    /**
     * Time in milliseconds.
     * Solely for use in serialization, deserialization, and inside this file.
     */
    public final long value;
    /**
     * Amount of time between which two times will be considered equal.
     */
    private final static long EPSILON = 15000;

    public IndexTime(long v) {
        value = v;
    }

    @Override
    public String toString() {
        return "T+" + value;
    }

    /**
     * Bumps a time forward. This is used to try to cancel out deletion records.
     */
    public IndexTime bumpedForward() {
        return new IndexTime(value + EPSILON + 1);
    }

    @Override
    public int compareTo(IndexTime other) {
        long diff = value - other.value;
        if (diff > EPSILON)
            return 1;
        if (diff < -EPSILON)
            return -1;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IndexTime)
            return compareTo((IndexTime) obj) == 0;
        return false;
    }
}
