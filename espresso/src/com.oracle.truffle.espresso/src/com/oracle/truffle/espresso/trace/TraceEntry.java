package com.oracle.truffle.espresso.trace;

import java.io.Serial;
import java.io.Serializable;

public class TraceEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = -3948103244520143317L;

    private final Serializable value;
    private final Class<?> type;

    public TraceEntry(Serializable value) {
        this.value = value;
        this.type = value.getClass();
    }

    public Serializable getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "TraceEntry{" +
                "value=" + value +
                ", type=" + type +
                '}';
    }
}
