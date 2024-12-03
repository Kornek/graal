package com.oracle.truffle.espresso.trace.io;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFileSystemProvider extends FileSystemProvider {
    private final Map<URI, FileSystem> fileSystems = new ConcurrentHashMap<>();

    @Override
    public String getScheme() {
        return "memory";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
        if (fileSystems.containsKey(uri)) {
            throw new FileSystemAlreadyExistsException();
        }
        FileSystem fs = new InMemoryFileSystem(this, uri);
        fileSystems.put(uri, fs);
        return fs;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        FileSystem fs = fileSystems.get(uri);
        if (fs == null) {
            throw new FileSystemNotFoundException();
        }
        return fs;
    }

    @Override
    public Path getPath(URI uri) {
        FileSystem fs = getFileSystem(uri);
        return fs.getPath(uri.getPath());
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        InMemoryPath inMemoryPath = (InMemoryPath) path;
        InMemoryFileSystem fs = (InMemoryFileSystem) inMemoryPath.getFileSystem();
        InMemoryFileStore store = (InMemoryFileStore) fs.getFileStore();

        if (options.contains(StandardOpenOption.CREATE_NEW)) {
            return store.createFile(inMemoryPath.toString());
        } else if (options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.WRITE)) {
            try {
                return store.openFile(inMemoryPath.toString(), options.contains(StandardOpenOption.APPEND));
            } catch (NoSuchFileException e) {
                return store.createFile(inMemoryPath.toString());
            }
        } else if (options.contains(StandardOpenOption.READ) || options.isEmpty()) {
            return store.openFile(inMemoryPath.toString(), false);
        } else {
            throw new UnsupportedOperationException("Unsupported open options: " + options);
        }
    }



    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
        return null;
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {

    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        InMemoryPath inMemoryPath = (InMemoryPath) path;
        InMemoryFileSystem fs = (InMemoryFileSystem) inMemoryPath.getFileSystem();
        InMemoryFileStore store = (InMemoryFileStore) fs.getFileStore();

        if (!store.fileExists(inMemoryPath.toString())) {
            throw new NoSuchFileException("File not found: " + inMemoryPath.toString());
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return Map.of();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

    }

}
