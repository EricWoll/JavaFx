package com.javafx.javafx.lib.Selection;

import com.javafx.javafx.lib.GraphNode.GraphNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GraphNodeSelectionManager {
    private static final GraphNodeSelectionManager instance = new GraphNodeSelectionManager();
    private final Set<GraphNode> selectedNodes = new HashSet<>();

    public static GraphNodeSelectionManager getInstance() {
        return instance;
    }

    public void select(GraphNode node) {
        clear();
        selectedNodes.add(node);
        node.setSelected(true);
    }

    public void selectMultiple(Collection<GraphNode> nodes) {
        selectedNodes.addAll(nodes);

        for (GraphNode node : nodes) {
            node.setSelected(true);
        }
    }

    public void clear() {
        for (GraphNode node : selectedNodes) {
            node.setSelected(false);
        }
        selectedNodes.clear();
    }

    public Set<GraphNode> getSelectedNodes() {
        return selectedNodes;
    }
}
