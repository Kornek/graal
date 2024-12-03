package com.oracle.truffle.espresso.trace.core;

import java.util.ArrayList;
import java.util.List;

class TraceTreeNode<T> {
    T value;
    TraceTreeNode<T> parent;
    List<TraceTreeNode<T>> children;
    boolean isCheckpoint;
    Object snapshot;

    private TraceTreeNode(T value) {
        this.value = value;
        this.children = new ArrayList<>();
        this.parent = null;
        this.snapshot = null;
        this.isCheckpoint = false;
    }

    private TraceTreeNode(Object snapshot, boolean isCheckpoint) {
        this.value = null; // Checkpoints have no specific value
        this.children = new ArrayList<>();
        this.parent = null;
        this.isCheckpoint = true;
        this.snapshot = snapshot;
    }

    public static <T> TraceTreeNode<T> createNode(T value) {
        return new TraceTreeNode<>(value);
    }

    // Factory method for creating a checkpoint node
    public static <T> TraceTreeNode<T> createCheckpoint(Object initialState) {
        return new TraceTreeNode<>(initialState, true);
    }

    public TraceTreeNode<T> addChild(T value) {
        TraceTreeNode<T> newChild = TraceTreeNode.createNode(value);
        newChild.parent = this;
        this.children.add(newChild);
        return newChild;
    }

    public TraceTreeNode<T> addCheckpoint(Object snapshot) {
        TraceTreeNode<T> newCheckpoint = TraceTreeNode.createCheckpoint(snapshot);
        newCheckpoint.parent = this;
        this.children.add(newCheckpoint);
        return newCheckpoint;
    }

    public TraceTreeNode<T> getParent() {
        return this.parent;
    }

    public List<TraceTreeNode<T>> getChildren() {
        return this.children;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
