package com.oracle.truffle.espresso.substitutions;

import com.oracle.truffle.espresso.meta.Meta;
import com.oracle.truffle.espresso.runtime.staticobject.StaticObject;
import com.oracle.truffle.espresso.trace.Tracer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

import static com.oracle.truffle.api.CompilerDirectives.*;

@EspressoSubstitutions
public final class Target_java_io_FileInputStream {

    /**
     * Mapping from guest FileInputStream to host FileInputStream.
     */
    private static HashMap<StaticObject, FileInputStream> guestToHost = new HashMap<>();

    @Substitution
    public static void initIDs() {
    }

    @TruffleBoundary
    @Substitution(hasReceiver = true)
    public static void open0(@JavaType(FileInputStream.class) StaticObject self, @JavaType(String.class) StaticObject name, @Inject Meta meta) {
        String hostName = meta.toHostString(name);
        if (!Files.exists(Path.of(hostName))) {
            throw meta.convertToGuestAndThrow(new FileNotFoundException(hostName));
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(hostName);
            guestToHost.put(self, fileInputStream);
        } catch (FileNotFoundException | SecurityException e) {
            throw meta.convertToGuestAndThrow(e);
        }
    }

    @TruffleBoundary
    @Substitution(hasReceiver = true)
    public static int read0(@JavaType(FileInputStream.class) StaticObject self, @Inject Meta meta) {
        InputStream in = getHostStream(self, meta);
        try {
            if (Tracer.isReplay() && Tracer.isTraced())
                return Tracer.reproduce("task1", "read0");
            int b = in.read();
            if (Tracer.isRecord() && Tracer.isTraced()) {
                Tracer.trace("task1", self.toString(), "read0", b);
            }
            return b;
        } catch (IOException e) {
            throw meta.convertToGuestAndThrow(e);
        }
    }

    @TruffleBoundary
    @Substitution(hasReceiver = true)
    public static int readBytes(@JavaType(FileInputStream.class) StaticObject self, @JavaType(byte[].class) StaticObject buffer, int off, int len, @Inject Meta meta) {
        InputStream in = getHostStream(self, meta);
        byte[] bytes = buffer.unwrap(meta.getLanguage());
        try {
            if (Tracer.isReplay() && Tracer.isTraced()) {
                byte[] reproduced = ((byte[]) Tracer.reproduce("task1", "readBytes")).clone();
                System.arraycopy(reproduced, 0, bytes, 0, len);
                System.out.println("Read from trace: " + Arrays.toString(bytes));
                return Tracer.reproduce("task1", "readBytes");
            }
            int numberOfBytesRead = in.read(bytes, off, len);
            if (Tracer.isRecord() && Tracer.isTraced()) {
                Tracer.trace("task1", self.toString(), "readBytes", bytes.clone());
                Tracer.trace("task1", self.toString(), "readBytes", numberOfBytesRead);
            }
            return numberOfBytesRead;
        } catch (IOException e) {
            throw meta.convertToGuestAndThrow(e);
        }
    }

    @TruffleBoundary
    @Substitution(hasReceiver = true)
    public static long skip0(@JavaType(FileInputStream.class) StaticObject self, long n, @Inject Meta meta) {
        InputStream in = getHostStream(self, meta);
        try {
            return in.skip(n);
        } catch (IOException e) {
            throw meta.convertToGuestAndThrow(e);
        }
    }

    @TruffleBoundary
    @Substitution(hasReceiver = true)
    public static int available0(@JavaType(FileInputStream.class) StaticObject self, @Inject Meta meta) {
        InputStream in = getHostStream(self, meta);
        try {
            return in.available();
        } catch (IOException e) {
            throw meta.convertToGuestAndThrow(e);
        }
    }

    private static InputStream getHostStream(@JavaType(FileInputStream.class) StaticObject self, Meta meta) {
        InputStream in = guestToHost.get(self);
        if (in == null) {
            int fd = Target_java_io_FileOutputStream.getFileDescriptor(self);
            if (fd == 0) {
                // reading from System.in
                in = meta.getContext().getEnv().in();
            }
        }
        return in;
    }

}
