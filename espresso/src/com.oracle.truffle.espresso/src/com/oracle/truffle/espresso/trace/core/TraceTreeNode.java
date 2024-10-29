package com.oracle.truffle.espresso.trace.core;

import java.util.ArrayList;
import java.util.List;

class TraceTreeNode<T> {
    T value;
    TraceTreeNode<T> parent;
    List<TraceTreeNode<T>> children;

    public TraceTreeNode(T value) {
        this.value = value;
        this.children = new ArrayList<>();
        this.parent = null;
    }

    public TraceTreeNode<T> addChild(T value) {
        TraceTreeNode<T> newChild = new TraceTreeNode<>(value);
        newChild.parent = this;
        this.children.add(newChild);
        return newChild;
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
