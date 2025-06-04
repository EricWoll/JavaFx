package com.javafx.javafx.Nodes;

import com.javafx.javafx.Nodes.Connectors.ConnectionManager;
import com.javafx.javafx.Nodes.Connectors.ConnectorPoint;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class Node extends StackPane {

    private static Node selectedNode;
    private final Shape background;
    private final ConnectionManager connectionManager;
    private final boolean allowUnlimitedConnections;
    private final int maxConnections;
    private double dragOffsetX;
    private double dragOffsetY;
    private final ConnectorPoint inputConnector;
    private final ConnectorPoint outputConnector;

    public Node(String title, int maxConnections, ConnectionManager connectionManager, Shape background) {
        this.allowUnlimitedConnections = maxConnections <= 0;
        this.maxConnections = maxConnections;
        this.connectionManager = connectionManager;
        this.background = background;

        inputConnector = new ConnectorPoint(this, connectionManager, ConnectorPoint.Type.INPUT);
        outputConnector = new ConnectorPoint(this, connectionManager, ConnectorPoint.Type.OUTPUT);



        Label label = new Label(title);
        label.setTextFill(Color.WHITE);

        getChildren().addAll(background, label);

        // Position connectors on the left and right edges vertically centered
        inputConnector.setTranslateX(-60);
        outputConnector.setTranslateX(60);

        getChildren().addAll(inputConnector, outputConnector);

        // Node dragging handlers
        final Delta dragDelta = new Delta();
        setOnMousePressed(e -> {
            if (!(e.getTarget() instanceof ConnectorPoint)) {
                clearSelection();
                selectedNode = this;
                background.setStroke(Color.YELLOW);

                dragDelta.x = getLayoutX() - e.getSceneX();
                dragDelta.y = getLayoutY() - e.getSceneY();

                e.consume();
            }
        });

        setOnMouseDragged(e -> {
            if (!(e.getTarget() instanceof ConnectorPoint)) {
                double newX = e.getSceneX() + dragDelta.x;
                double newY = e.getSceneY() + dragDelta.y;

                setLayoutX(newX);
                setLayoutY(newY);

                connectionManager.updateConnections();

                e.consume();
            }
        });

    }

    private void selectAndStartDrag(MouseEvent e) {
        clearSelection();
        selectedNode = this;
        background.setStroke(Color.YELLOW);
        dragOffsetX = e.getSceneX() - getLayoutX();
        dragOffsetY = e.getSceneY() - getLayoutY();
        e.consume();
    }

    private void handleDrag(MouseEvent e) {
        setLayoutX(e.getSceneX() - dragOffsetX);
        setLayoutY(e.getSceneY() - dragOffsetY);
        connectionManager.updateConnections(); // Optional: to update line positions when dragging
        e.consume();
    }

    private void select(MouseEvent e) {
        clearSelection();
        selectedNode = this;
        background.setStroke(Color.YELLOW);
        e.consume();
    }

    public static void clearSelection() {
        if (selectedNode != null) {
            selectedNode.background.setStroke(Color.TRANSPARENT);
            selectedNode = null;
        }
    }

    public ConnectorPoint getConnector(ConnectorPoint.Type type) {
        return (type == ConnectorPoint.Type.INPUT) ? inputConnector : outputConnector;
    }

    public boolean allowsMoreConnections(int current) {
        return allowUnlimitedConnections || current < maxConnections;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    private static class Delta {
        double x, y;
    }
}
