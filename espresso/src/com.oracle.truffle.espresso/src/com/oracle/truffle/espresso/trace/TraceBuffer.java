package com.oracle.truffle.espresso.trace;

import java.io.*;
import java.util.*;

class TraceBuffer implements Serializable {
    @Serial
    private static final long serialVersionUID = 8971472849389101936L;


    //    private AtomicInteger counter = new AtomicInteger(0);
    //Replace with tree structure
    private Map<String, Queue<TraceEntry>> buffer = new HashMap<>();
    private String sourceCodeSignature;

    public void setSourceCodeSignature(String signature) {
        this.sourceCodeSignature = signature;
    }

    private Queue<TraceEntry> get(String taskId) {
        return buffer.computeIfAbsent(taskId, k -> new LinkedList<>());
    }

    protected void clear() {
//        counter = new AtomicInteger(0);
        buffer.clear();
    }

    protected void record(String taskId, String clazz, String function, Serializable value) {
        get(taskId).add(new TraceEntry(value));
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

    protected TraceEntry getNextValue(String taskId, String function) {
        Queue<TraceEntry> traceQueue = buffer.get(taskId);
        if (traceQueue != null && !traceQueue.isEmpty()) {
            return traceQueue.remove();
        }
        return null;
    }

    protected boolean isEmpty(String taskId) {
        Queue<TraceEntry> traceQueue = buffer.get(taskId);
        return traceQueue == null  || traceQueue.isEmpty();
    }
}
