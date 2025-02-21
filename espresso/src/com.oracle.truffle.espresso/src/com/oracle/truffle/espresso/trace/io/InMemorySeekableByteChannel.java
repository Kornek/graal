package com.oracle.truffle.espresso.trace.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

public class InMemorySeekableByteChannel implements SeekableByteChannel {
    private byte[] data;
    private int position;
    private boolean open;
    private final InMemoryFileStore fileStore;
    private final String path;

    public InMemorySeekableByteChannel(byte[] data, InMemoryFileStore fileStore, String path, boolean append) {
        this.data = data;
        this.position = append ? data.length : 0;
        this.open = true;
        this.fileStore = fileStore;
        this.path = path;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureOpen();
        if (position >= data.length) {
            return -1; // End of file
        }
        int bytesToRead = Math.min(dst.remaining(), data.length - position);
        dst.put(data, position, bytesToRead);
        position += bytesToRead;
        return bytesToRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        ensureOpen();
        int bytesToWrite = src.remaining();
        ensureCapacity(position + bytesToWrite);
        src.get(data, position, bytesToWrite);
        position += bytesToWrite;
        fileStore.updateFile(path, data);
        return bytesToWrite;
    }

    @Override
    public long position() throws IOException {
        ensureOpen();
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        ensureOpen();
        if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid position");
        }
        position = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return data.length;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        ensureOpen();
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size");
        }
        if (size < data.length) {
            data = Arrays.copyOf(data, (int) size);
        }
        if (position > size) {
            position = (int) size;
        }
        fileStore.updateFile(path, data);
        return this;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        open = false;
    }

    private void ensureOpen() throws IOException {
        if (!open) {
            throw new IOException("Channel is closed");
        }
    }

    private void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity > data.length) {
            data = Arrays.copyOf(data, requiredCapacity);
        }
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length); // Return a copy of the data
    }
}
