package com.oracle.truffle.espresso.trace;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

class TraceBuffer implements Serializable {
    @Serial
    private static final long serialVersionUID = 8971472849389101936L;

    private Map<String, Queue<TraceEntry>> buffer = new HashMap<>();


    private Queue<TraceEntry> get(String branch) {
        return buffer.computeIfAbsent(branch, k -> new LinkedList<>());
    }

    protected void clear() {
        buffer.clear();
    }

    protected void record(String branch, String clazz, String function, Serializable value) {
        get(branch).add(new TraceEntry(value));
    }

    protected void persistToDisk(Path path) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path.toString()))) {
            out.writeObject(this.buffer);
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadFromDisk(Path path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(path.toString()))) {
            this.buffer = (Map<String, Queue<TraceEntry>>) in.readObject();
        } catch (IOException | ClassNotFoundException ignored) {
            // buffer not set
        }
    }

    protected TraceEntry getNextValue(String branch, String function) {
        Queue<TraceEntry> traceQueue = buffer.get(branch);
        if (traceQueue != null && !traceQueue.isEmpty()) {
            return traceQueue.remove();
        }
        return null;
    }

    protected boolean isEmpty(String branch) {
        Queue<TraceEntry> traceQueue = buffer.get(branch);
        return traceQueue == null  || traceQueue.isEmpty();
    }

    protected Map<String, Queue<TraceEntry>> getBuffer() {
        return buffer;
    }
}
