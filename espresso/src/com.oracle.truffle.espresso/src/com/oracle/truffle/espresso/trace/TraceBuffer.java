package com.oracle.truffle.espresso.trace;

import java.io.*;
import java.util.*;

class TraceBuffer implements Serializable {
    @Serial
    private static final long serialVersionUID = 8971472849389101936L;


    //    private AtomicInteger counter = new AtomicInteger(0);
    //Replace with tree structure
    private Queue<TraceEntry> buffer = new LinkedList<>();
    private String sourceCodeSignature;

    public void setSourceCodeSignature(String signature) {
        this.sourceCodeSignature = signature;
    }

    private Queue<TraceEntry> get() {
        return buffer;
    }

    protected void clear() {
//        counter = new AtomicInteger(0);
        buffer.clear();
    }

    protected void record(String clazz, String function, Serializable value) {
        get().add(new TraceEntry(value));
    }

    protected void persistToDisk(File file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(this);
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadFromDisk(File filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            TraceBuffer loadedTraceBuffer = (TraceBuffer) in.readObject();
            this.buffer = loadedTraceBuffer.buffer;
            this.sourceCodeSignature = loadedTraceBuffer.sourceCodeSignature;
        }
    }

    protected TraceEntry getNextValue() {
        Queue<TraceEntry> traceQueue = buffer;
        if (traceQueue != null && !traceQueue.isEmpty()) {
            return traceQueue.remove();
        }
        return null;
    }

    protected boolean isEmpty() {
        Queue<TraceEntry> traceQueue = buffer;
        return traceQueue == null  || traceQueue.isEmpty();
    }
}
