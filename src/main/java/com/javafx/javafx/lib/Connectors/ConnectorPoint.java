package com.javafx.javafx.lib.Connectors;


import com.javafx.javafx.lib.GraphNode.GraphNode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ConnectorPoint extends Rectangle {

    public enum Type { INPUT, OUTPUT }

    private final GraphNode parentGraphNode;
    private final ConnectionManager manager;
    private final Type type;

    public ConnectorPoint(GraphNode parentGraphNode, ConnectionManager manager, Type type) {
        super(10, 20);
        this.parentGraphNode = parentGraphNode;
        this.manager = manager;
        this.type = type;

        setArcWidth(5);
        setArcHeight(5);
        setFill(type == Type.INPUT ? Color.GREEN : Color.ORANGE);

        setOnMousePressed(this::onPressed);
        setOnDragDetected(this::onDragStart);
    }

    private void onPressed(MouseEvent e) {
        manager.handleConnectorClick(this);
        e.consume(); // Prevent node selection
    }

    private void onDragStart(MouseEvent e) {
        manager.startConnection(this);
        e.consume();
    }

    public GraphNode getParentNode() {
        return parentGraphNode;
    }

    public Type getType() {
        return type;
    }

}
