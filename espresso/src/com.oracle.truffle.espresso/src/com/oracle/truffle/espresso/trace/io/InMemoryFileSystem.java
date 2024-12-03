package com.oracle.truffle.espresso.trace.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

public class InMemoryFileSystem extends FileSystem {
    private final InMemoryFileSystemProvider provider;
    private final URI uri;
    private final String separator = "/";
    private boolean open = true;
    private final InMemoryFileStore fileStore;

    public InMemoryFileSystem(InMemoryFileSystemProvider provider, URI uri) {
        this.provider = provider;
        this.uri = uri;
        this.fileStore = new InMemoryFileStore();

    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return separator;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(new InMemoryPath(this, separator));
    }

    public FileStore getFileStore() {
        return fileStore;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.singleton(fileStore);
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Set.of();
    }

    @Override
    public Path getPath(String first, String... more) {
        String path = String.join(separator, first, String.join(separator, more));
        return new InMemoryPath(this, path);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return null;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return null;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return null;
    }

    public void clearFileStore() {
        fileStore.clearStore();
    }

    public void initStoreFromDisk(File file) throws IOException, ClassNotFoundException {
        fileStore.loadFromFile(file);
    }

    public void persistStoreToDisk(File file) throws IOException {
        fileStore.saveToFile(file);
    }
}
