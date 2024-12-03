package com.oracle.truffle.espresso.trace.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.regex.Pattern;

public class InMemoryPath implements Path {
    private final InMemoryFileSystem fileSystem;
    private final String path;

    public InMemoryPath(InMemoryFileSystem fileSystem, String path) {
        this.fileSystem = fileSystem;
        this.path = path;
    }

    @Override
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return path.startsWith(fileSystem.getSeparator());
    }

    @Override
    public Path getRoot() {
        return new InMemoryPath(fileSystem, fileSystem.getSeparator());
    }

    @Override
    public Path getFileName() {
        int index = path.lastIndexOf(fileSystem.getSeparator());
        return new InMemoryPath(fileSystem, path.substring(index + 1));
    }

    @Override
    public Path getParent() {
        int index = path.lastIndexOf(fileSystem.getSeparator());
        if (index <= 0) return null;
        return new InMemoryPath(fileSystem, path.substring(0, index));
    }

    @Override
    public int getNameCount() {
        return path.split(Pattern.quote(fileSystem.getSeparator())).length;
    }

    @Override
    public Path getName(int index) {
        return null;
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return null;
    }

    @Override
    public boolean startsWith(Path other) {
        return false;
    }

    @Override
    public boolean endsWith(Path other) {
        return false;
    }

    @Override
    public Path normalize() {
        return null;
    }

    @Override
    public Path resolve(Path other) {
        return null;
    }

    @Override
    public Path relativize(Path other) {
        return null;
    }

    @Override
    public URI toUri() {
        return null;
    }

    @Override
    public Path toAbsolutePath() {
        return null;
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return null;
    }

    @Override
    public int compareTo(Path other) {
        return 0;
    }

    @Override
    public String toString() {
        return path;
    }
}

