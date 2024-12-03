package com.oracle.truffle.espresso.trace.core;

import java.util.ArrayList;
import java.util.List;

public class TraceTree<T> {
    TraceTreeNode<T> root;
    TraceTreeNode<T> currentNode;
    List<TraceTreeNode<T>> checkpoints;

    public TraceTree() {
        this.root = TraceTreeNode.createCheckpoint(null);
        this.currentNode = root; // Start at the root
        this.checkpoints = new ArrayList<>();
    }

    public void traverseToChild(int childIndex) {
        if (childIndex >= 0 && childIndex < currentNode.getChildren().size()) {
            currentNode = currentNode.getChildren().get(childIndex);
        } else {
            System.out.println("Invalid child index");
        }
    }

    public void traverseToParent() {
        if (currentNode.getParent() != null) {
            currentNode = currentNode.getParent();
        } else {
            System.out.println("Already at the root");
        }
    }

    public void addBranch(T value) {
        currentNode.addChild(value);
    }

    public void addCheckpoint(Object initialState) {
        TraceTreeNode<T> checkpoint = currentNode.addCheckpoint(initialState);
        checkpoints.add(checkpoint); // Store the checkpoint for easy access
    }

    // Traverse directly to a checkpoint by index
    public void traverseToCheckpoint(int checkpointIndex) {
        if (checkpointIndex >= 0 && checkpointIndex < checkpoints.size()) {
            currentNode = checkpoints.get(checkpointIndex);
        } else {
            System.out.println("Invalid checkpoint index");
        }
    }

    public T getCurrentNodeValue() {
        return currentNode.value;
    }

    public void printTree(TraceTreeNode<T> node, String indent) {
        System.out.println(indent + node);
        for (TraceTreeNode<T> child : node.getChildren()) {
            printTree(child, indent + "  ");
        }
    }

    public void printTree() {
        printTree(root, "");
    }
}