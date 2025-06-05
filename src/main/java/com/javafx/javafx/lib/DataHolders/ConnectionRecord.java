package com.javafx.javafx.lib.DataHolders;

import com.javafx.javafx.lib.GraphNode.GraphNode;
import javafx.scene.shape.Line;

public record ConnectionRecord(GraphNode from, GraphNode to, Line line) {
}
