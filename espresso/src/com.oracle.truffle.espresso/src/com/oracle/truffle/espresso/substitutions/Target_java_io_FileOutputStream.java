package com.oracle.truffle.espresso.substitutions;


import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.espresso.descriptors.Symbol;
import com.oracle.truffle.espresso.impl.Field;
import com.oracle.truffle.espresso.meta.Meta;
import com.oracle.truffle.espresso.runtime.staticobject.StaticObject;
import com.oracle.truffle.espresso.trace.Tracer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@EspressoSubstitutions
final public class Target_java_io_FileOutputStream {
    private static final HashMap<StaticObject, OutputStream> guestToHost = new HashMap<>();
    private static final Set<File> openFiles = new HashSet<>();

    @Substitution
    public static void initIDs() {
        /* nop */
    }

    /**
     * Returns a list of files of open streams.
     *
     * @return a list of files of open streams
     */
    public static Set<File> openFiles() {
        return new HashSet<>(openFiles);
    }

    public static void resetAuxiliaryStructures() {
        guestToHost.forEach((guest, host) -> {
            try {
                host.close();
            } catch (IOException ignored) {
            }
        });

        guestToHost.clear();
        openFiles.clear();
    }

    @CompilerDirectives.TruffleBoundary
    @Substitution(hasReceiver = true)
    public static void open0(@JavaType(FileOutputStream.class) StaticObject self, @JavaType(String.class) StaticObject name, boolean append, @Inject Meta meta) {
        String hostName = meta.toHostString(name);
        try {
            OutputStream outputStream = new FileOutputStream(hostName, append);
            guestToHost.put(self, outputStream);
            openFiles.add(new File(hostName));
            // modified time was probably not updated when hostName already existed
            Files.setLastModifiedTime(Path.of(hostName), FileTime.from(Instant.now()));
        } catch (FileNotFoundException | SecurityException ex) {
            throw meta.convertToGuestAndThrow(ex);
        } catch (IOException ex) {
            throw meta.convertToGuestAndThrow(new FileNotFoundException(ex.getMessage()));
        }
    }

    @CompilerDirectives.TruffleBoundary
    @Substitution(hasReceiver = true)
    public static void writeBytes(@JavaType(FileOutputStream.class) StaticObject self, @JavaType(byte[].class) StaticObject bytes, int offset, int len, boolean append, @Inject Meta meta) {
        int fd = getFileDescriptor(self);

        if (fd == 1 || fd == 2) {
            // writing to standard output or error stream
            OutputStream known = (fd == 1) ? meta.getContext().getEnv().out() : meta.getContext().getEnv().err();
            try {
                byte[] buffer;
                if (Tracer.isReplay() && Tracer.isTraced()) {
                    buffer = ((byte[]) Tracer.reproduce("task1", "writeBytes")).clone();
                } else {
                    buffer = bytes.unwrap(meta.getLanguage());
                }
                if (Tracer.isRecord() && Tracer.isTraced()) {
                    Tracer.trace("task1", self.toString(), "writeBytes", buffer.clone());
                }
                write(known, buffer, offset, len);
            } catch (IOException ex) {
                throw meta.convertToGuestAndThrow(ex);
            }
        } else {
            // writing to other stream
            OutputStream stream = guestToHost.get(self);
            try {
                byte[] buffer;
                if (Tracer.isReplay() && Tracer.isTraced()) {
                    buffer = ((byte[]) Tracer.reproduce("task1", "writeBytes")).clone();
                } else {
                    buffer = bytes.unwrap(meta.getLanguage());
                }
                if (Tracer.isRecord() && Tracer.isTraced()) {
                    Tracer.trace("task1", self.toString(), "writeBytes", buffer.clone());
                }
                write(stream, buffer, offset, len);
            } catch (IOException ex) {
                throw meta.convertToGuestAndThrow(ex);
            }
        }
    }

    static int getFileDescriptor(@JavaType(FileOutputStream.class) StaticObject self) {
        StaticObject fileDescriptor = getFileDescriptorObject(self);
        Field fdField = fileDescriptor.getKlass().lookupDeclaredField(Symbol.Name.fd, Symbol.Type._int);
        return (int) fdField.get(fileDescriptor);
    }

    static StaticObject getFileDescriptorObject(@JavaType(FileOutputStream.class) StaticObject self) {
        Field fileDescriptorField = self.getKlass().lookupDeclaredField(Symbol.Name.fd, Symbol.Type.java_io_FileDescriptor);
        return (StaticObject) fileDescriptorField.get(self);
    }

    @CompilerDirectives.TruffleBoundary
    private static void write(OutputStream out, byte[] bytes, int offset, int len) throws IOException {
        out.write(bytes, offset, len);
    }
}

