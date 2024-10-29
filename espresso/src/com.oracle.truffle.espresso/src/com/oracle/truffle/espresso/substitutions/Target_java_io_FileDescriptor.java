package com.oracle.truffle.espresso.substitutions;


import com.oracle.truffle.espresso.runtime.staticobject.StaticObject;

import java.io.FileDescriptor;

@EspressoSubstitutions
public final class Target_java_io_FileDescriptor {

    @Substitution
    public static void initIDs() {
    }

    /*
     * On Windows return the handle for the standard streams.
     */
    @Substitution
    public static long getHandle(int d) {
        return -1;
    }

    /**
     * Returns true, if the file was opened for appending.
     */
    @Substitution
    public static boolean getAppend(int fd) {
        return true;
    }

    @Substitution(hasReceiver = true)
    public static void close0(@JavaType(FileDescriptor.class) StaticObject self) {
       // noop.
    }
}
