package com.oracle.truffle.espresso.trace.core;

public class TraceTree<T> {
    TraceTreeNode<T> root;
    TraceTreeNode<T> currentNode;

    public TraceTree(T rootValue) {
        this.root = new TraceTreeNode<>(rootValue);
        this.currentNode = root; // Start at the root
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