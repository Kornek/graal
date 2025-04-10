package com.oracle.truffle.espresso.trace;

import com.oracle.truffle.api.Truffle;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.espresso.EspressoLanguage;
import com.oracle.truffle.espresso.trace.io.InMemoryFileStore;
import com.oracle.truffle.espresso.trace.io.InMemoryFileSystem;
import com.oracle.truffle.espresso.trace.io.InMemoryFileSystemProvider;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Tracer {

    private static final String TRACE_FILENAME = "trace.bin";
    private static final String TRACE_FILESTORE_NAME = "trace_filestore.bin";
    private static final TruffleLogger logger = TruffleLogger.getLogger(EspressoLanguage.ID, Tracer.class);
    private static final InMemoryFileSystem snapshotFileSystem;
    private static final InMemoryFileSystem restoreFileSystem;
    private static final TraceBuffer buffer = new TraceBuffer();
    private static final TraceBuffer replayBuffer = new TraceBuffer();
    private static String currentBranch;
    private static String tracedNode;

    private static TraceMode traceMode = TraceMode.OFF;
    static {
        // fs
        try {
            URI uri = URI.create("memory:///");
            URI restoreUri = URI.create("restore:///");
            FileSystemProvider provider = new InMemoryFileSystemProvider();
            snapshotFileSystem = (InMemoryFileSystem) provider.newFileSystem(uri, Collections.emptyMap());
            restoreFileSystem = (InMemoryFileSystem) provider.newFileSystem(restoreUri, Collections.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Tracer() {
    }

    public static FileSystem getSnapshotFileSystem() {
        return snapshotFileSystem;
    }

    public static FileSystem getRestoreFileSystem() {
        return restoreFileSystem;
    }

    enum TraceMode {
        OFF,
        RECORD,
        REPLAY
    }

    private static void setTraceMode(TraceMode traceType) {
        Tracer.traceMode = traceType;
    }

    public static void setTracedNode(String tracedNode) {
        Tracer.tracedNode = tracedNode;
    }
    public static void setCurrentBranch(String branch) {
        Tracer.currentBranch = branch;
    }

//    public static void turnOff() {
//        replayBuffer.clear();
//        setTraceMode(TraceMode.OFF);
//         logger.log(Level.FINE, "Trace turned OFF.");
//    }

    public static void startRecording() {
//        replayBuffer.clear();
//        snapshotFileSystem.clearFileStore();
        setTraceMode(TraceMode.RECORD);
        logger.log(Level.FINE,"Trace recording started.");
    }

    public static void continueRecording() {
        setTraceMode(TraceMode.RECORD);
        logger.log(Level.FINE,"Trace recording continued.");
    }

    public static void continueReplaying() {
        setTraceMode(TraceMode.REPLAY);
        copyFileStoreContents(snapshotFileSystem, restoreFileSystem);
        copyTraceBuffer(buffer, replayBuffer);
        logger.log(Level.FINE, "Trace replaying continued.");
    }

    public static void initTraceSession() {
        buffer.clear();
        replayBuffer.clear();
        snapshotFileSystem.clearFileStore();
        restoreFileSystem.clearFileStore();
        setTraceMode(TraceMode.RECORD);
        logger.log(Level.FINE, "Trace session initialized.");
    }

    public static void initReplaySession(Path path) {
        initTraceSession();
        try {
            buffer.loadFromDisk(path.resolve(TRACE_FILENAME));
            snapshotFileSystem.initStoreFromDisk(path.resolve(TRACE_FILESTORE_NAME));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        continueReplaying();
    }

    public static boolean isReplay() {
        return traceMode == TraceMode.REPLAY;
    }

    public static boolean isRecord() {
        return traceMode == TraceMode.RECORD;
    }

    public static boolean shouldTraceNode() {
        Boolean result = Truffle.getRuntime().iterateFrames(frameInstance -> {
            Node callNode = frameInstance.getCallNode();
            if (callNode != null) {
                RootNode rootNode = callNode.getRootNode();
                if (rootNode.getName().equals(tracedNode)) {
                    return true;
                }
            }
            return null;
        });
        return result != null && result;
    }

    public static void trace(String clazz, String function, Serializable value) {
        logger.log(Level.FINEST, "Traced value: Task=%s, Class=%s, Function=%s, Value=%s"
                .formatted(currentBranch, clazz, function, getArrayRepresentation(value)));
        buffer.record(currentBranch, clazz, function, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T reproduce( String function) {
        try {
            TraceEntry entry = replayBuffer.getNextValue(currentBranch, function);
            logger.log(Level.FINEST, "Reproduced value: Task=%s, Function=%s, Value=%s"
                    .formatted(currentBranch, function, getArrayRepresentation(entry.getValue())));
            return (T) entry.getType().cast(entry.getValue());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static boolean hasRemainingTrace() {
        boolean remaining = !replayBuffer.isEmpty(currentBranch);
        if(!remaining) {
            Tracer.continueRecording();
        }
        return remaining;
    }

    public static void save(Path path) {
        try {
            // if path not exists, create it
            if(!Files.exists(path)) {
                Files.createDirectories(path);
            }

            buffer.persistToDisk(path.resolve(TRACE_FILENAME));
            snapshotFileSystem.persistStoreToDisk(path.resolve(TRACE_FILESTORE_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getArrayRepresentation(Serializable value) {
        if (value != null) {
            if (value.getClass().isArray()) {
                int maxElements = Math.min(64, Array.getLength(value));

                if (value instanceof int[]) {
                    return Arrays.toString(Arrays.copyOf((int[]) value, maxElements));
                } else if (value instanceof long[]) {
                    return Arrays.toString(Arrays.copyOf((long[]) value, maxElements));
                } else if (value instanceof double[]) {
                    return Arrays.toString(Arrays.copyOf((double[]) value, maxElements));
                } else if (value instanceof boolean[]) {
                    return Arrays.toString(Arrays.copyOf((boolean[]) value, maxElements));
                } else if (value instanceof char[]) {
                    return Arrays.toString(Arrays.copyOf((char[]) value, maxElements));
                } else if (value instanceof byte[]) {
                    return Arrays.toString(Arrays.copyOf((byte[]) value, maxElements));
                } else if (value instanceof float[]) {
                    return Arrays.toString(Arrays.copyOf((float[]) value, maxElements));
                } else if (value instanceof short[]) {
                    return Arrays.toString(Arrays.copyOf((short[]) value, maxElements));
                } else {
                    // For Object arrays, handle with deepToString for multi-dimensional arrays
                    Object[] objectArray = (Object[]) value;
                    Object[] truncatedArray = Arrays.copyOf(objectArray, maxElements);
                    return Arrays.deepToString(truncatedArray);
                }
            }
            return value.toString();
        }
        return null;
    }

    private static void copyTraceBuffer(TraceBuffer source, TraceBuffer destination) {
        for (Map.Entry<String, Queue<TraceEntry>> entry : source.getBuffer().entrySet()) {
            String branch = entry.getKey();
            Queue<TraceEntry> sourceQueue = entry.getValue();

            // Create a new queue and copy elements to avoid modifying the original queue
            Queue<TraceEntry> newQueue = new LinkedList<>(sourceQueue);

            // Store the copied queue in the destination buffer
            destination.getBuffer().put(branch, newQueue);
        }
    }


    private static void copyFileStoreContents(InMemoryFileSystem snapshotFileSystem, InMemoryFileSystem restoreFileSystem) {
        InMemoryFileStore snapshotStore = (InMemoryFileStore) snapshotFileSystem.getFileStore();
        InMemoryFileStore restoreStore = (InMemoryFileStore) restoreFileSystem.getFileStore();

        for (Map.Entry<String, byte[]> entry : snapshotStore.getFiles().entrySet()) {
            String path = entry.getKey();
            byte[] data = entry.getValue();

            try {
                if (!restoreStore.fileExists(path)) {
                    restoreStore.createFile(path);
                }
                restoreStore.updateFile(path, data);
            } catch (IOException e) {
                logger.log(Level.SEVERE,"Failed to copy file: " + path);
                e.printStackTrace();
            }
        }
    }
}
