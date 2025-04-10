package com.oracle.truffle.espresso.trace.io;


import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFileStore extends FileStore implements Serializable {

    @Serial
    private static final long serialVersionUID = -7407056589376145304L;

    private ConcurrentHashMap<String, byte[]> files = new ConcurrentHashMap<>();

    public InMemoryFileStore() {
    }

    public SeekableByteChannel createFile(String path) throws IOException {
        if (files.containsKey(path)) {
            throw new FileAlreadyExistsException("File already exists: " + path);
        }
        byte[] bytes = new byte[0];
        files.put(path, bytes);
        return new InMemorySeekableByteChannel(bytes, this, path, false);
    }

    public SeekableByteChannel openFile(String path, boolean append) throws IOException {
        byte[] bytes = files.get(path);
        if (bytes == null) {
            throw new NoSuchFileException("File not found: " + path);
        }
        return new InMemorySeekableByteChannel(bytes, this, path, append);
    }

    public SeekableByteChannel truncateFile(String path) throws IOException {
        if (!files.containsKey(path)) {
            throw new NoSuchFileException("File not found: " + path);
        }
        byte[] emptyBytes = new byte[0];
        files.put(path, emptyBytes);
        return new InMemorySeekableByteChannel(emptyBytes, this, path, false);
    }

    public void updateFile(String path, byte[] data) {
        files.put(path, data);
    }

    public void deleteFile(String path) throws IOException {
        if (files.remove(path) == null) {
            throw new NoSuchFileException("File not found: " + path);
        }
    }


    public boolean fileExists(String path) {
        return files.containsKey(path);
    }

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public String type() {
        return "memory";
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return 0;
    }

    @Override
    public long getUsableSpace() throws IOException {
        return 0;
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        return 0;
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return false;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        return false;
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return null;
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        return null;
    }

    public void saveToFile(Path path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path.toString()))) {
            oos.writeObject(this);
        }
    }

    public void loadFromFile(Path path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path.toString()))) {
            InMemoryFileStore store = (InMemoryFileStore) ois.readObject();
            this.files = store.files;
        } catch (IOException | ClassNotFoundException ignored) {
            // files not set
        }
    }

    public void clearStore() {
        this.files.clear();
    }

    public ConcurrentHashMap<String, byte[]> getFiles() {
        return files;
    }
}
