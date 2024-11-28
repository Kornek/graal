package com.oracle.truffle.espresso.trace;

import com.oracle.truffle.api.Truffle;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Tracer {

    private static final Logger logger = Logger.getLogger(Tracer.class.getName());

    static {
        try {
            String userPath = System.getProperty("user.home");
            FileHandler fileHandler = new FileHandler(userPath + "/tracer.log", true); // 'true' for append mode

            fileHandler.setFormatter(new SimpleFormatter());

            logger.addHandler(fileHandler);

            logger.setLevel(Level.INFO);

        } catch (Exception e) {
            logger.severe("Failed to initialize logger file handler: " + e.getMessage());
        }
    }

    public Tracer() {
    }

    enum TraceMode {
        OFF,
        RECORD,
        REPLAY
    }

    private static String tracedNode;
    private static String sourceCodeSignature;  // Store the source code hash
    private static final TraceBuffer buffer = new TraceBuffer();

    private static TraceMode traceMode = TraceMode.OFF;

    private static void setTraceMode(TraceMode traceType) {
        Tracer.traceMode = traceType;
    }

    public static void setTracedNode(String tracedNode) {
        Tracer.tracedNode = tracedNode;
    }

    public static void turnOff() {
        buffer.clear();
        setTraceMode(TraceMode.OFF);
        System.out.println("Trace turned OFF.");
    }

    public static void startRecording() {
        buffer.clear();
        setTraceMode(TraceMode.RECORD);
        System.out.println("Trace recording started.");
    }

    public static void startReplaying(File file) {
        buffer.clear();
        try {
            buffer.loadFromDisk(file.getAbsolutePath());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        setTraceMode(TraceMode.REPLAY);
        System.out.println("Trace replaying started.");
    }

    public static boolean isReplay(String task1) {
        return traceMode == TraceMode.REPLAY && hasNextValue(task1);
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

    public static void trace(String taskId, String clazz, String function, Serializable value) {
        logger.info(() -> "Traced value: Task=%s, Class=%s, Function=%s, Value=%s"
                .formatted(taskId, clazz, function, getArrayRepresentation(value)));
        buffer.record(taskId, clazz, function, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T reproduce(String task, String function) {
        try {
            TraceEntry entry = buffer.getNextValue(task, function);
            logger.info(() -> "Reproduced value: Task=%s, Function=%s, Value=%s"
                    .formatted(task, function, getArrayRepresentation(entry.getValue())));
            return (T) entry.getType().cast(entry.getValue());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static boolean hasNextValue(String taskId) {
        if (buffer.isEmpty(taskId)) {
            turnOff();
            return false;
        }
        return true;
    }

    public static void save(File file) {
        try {
            buffer.persistToDisk(file);
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
}
