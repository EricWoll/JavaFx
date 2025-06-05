package com.javafx.javafx.lib.GraphNode;

import com.javafx.javafx.lib.Connectors.ConnectionManager;
import com.javafx.javafx.lib.Connectors.ConnectorPoint;
import com.javafx.javafx.lib.Selection.GraphNodeSelectionManager;
import com.javafx.javafx.lib.Menu.MenuHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

import java.util.HashMap;
import java.util.Map;

public class GraphNode extends StackPane {

    private final Shape background;
    private final AnchorPane canvas;
    private final ConnectionManager connectionManager;
    private final boolean allowUnlimitedConnections;
    private final int maxConnections;
    private final ConnectorPoint inputConnector;
    private final ConnectorPoint outputConnector;
    private ContextMenu contextMenu;
    private double lastMouseX, lastMouseY;
    private double startMouseX, startMouseY;
    private final Map<GraphNode, Point2D> graphNodePositions = new HashMap<>();
    private final GraphNodeSelectionManager selectionManager = GraphNodeSelectionManager.getInstance();
    private final AnchorPane wrapperPane;


    public GraphNode(String title, int maxConnections, ConnectionManager connectionManager, Shape background, AnchorPane canvas, AnchorPane wrapperPane) {
        this.allowUnlimitedConnections = maxConnections <= 0;
        this.maxConnections = maxConnections;
        this.connectionManager = connectionManager;
        this.background = background;
        this.canvas = canvas;
        this.wrapperPane = wrapperPane;

        inputConnector = new ConnectorPoint(this, connectionManager, ConnectorPoint.Type.INPUT);
        outputConnector = new ConnectorPoint(this, connectionManager, ConnectorPoint.Type.OUTPUT);

        menu();

        Label label = new Label(title);
        label.setTextFill(Color.WHITE);

        getChildren().addAll(background, label);

        // Position connectors on the left and right edges vertically centered
        inputConnector.setTranslateX(-60);
        outputConnector.setTranslateX(60);

        getChildren().addAll(inputConnector, outputConnector);

        // Node dragging handlers
        // Assuming this code is inside GraphNode class

        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnMouseReleased(e -> {
            graphNodePositions.clear();
            canvas.setCursor(Cursor.DEFAULT);
        });


        canvas.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DELETE, BACK_SPACE -> {
                    deleteSelectedNodes(canvas, connectionManager);
                }
            }
        });

    }

    private void menu() {
        MenuItem deleteNodeItem = new MenuItem("delete Node");
        contextMenu = new ContextMenu(deleteNodeItem);
        contextMenu.setAutoHide(true);

        new MenuHandler.Builder(this, contextMenu).useRightClick(true).build();

        deleteNodeItem.setOnAction(e -> {
            deleteSelectedNodes(canvas, connectionManager);
        });
    }

    private void onMousePressed(MouseEvent e) {
        if (!(e.getTarget() instanceof ConnectorPoint)) {
            switch (e.getButton()) {
                case PRIMARY -> {
                    canvas.setCursor(Cursor.CLOSED_HAND);
                    if (!selectionManager.getSelectedNodes().contains(this)) {
                        selectionManager.clear();
                        selectionManager.select(this);
                    }

                    // Convert scene coordinates to local coords of the wrapperPane (AnchorPane)
                    Point2D parentPoint = wrapperPane.sceneToLocal(e.getSceneX(), e.getSceneY());

                    startMouseX = parentPoint.getX();
                    startMouseY = parentPoint.getY();

                    graphNodePositions.clear();
                    for (GraphNode node : selectionManager.getSelectedNodes()) {
                        graphNodePositions.put(node, new Point2D(node.getLayoutX(), node.getLayoutY()));
                    }

                    e.consume();
                }
                case SECONDARY -> {
                    contextMenu.show(this, e.getScreenX(), e.getScreenY());
                    e.consume();
                }
            }
        }
    }

    private void onMouseDragged(MouseEvent e) {
        if (!(e.getTarget() instanceof ConnectorPoint)) {
            Point2D parentPoint = wrapperPane.sceneToLocal(e.getSceneX(), e.getSceneY());

            double deltaX = parentPoint.getX() - startMouseX;
            double deltaY = parentPoint.getY() - startMouseY;

            for (Map.Entry<GraphNode, Point2D> entry : graphNodePositions.entrySet()) {
                GraphNode node = entry.getKey();
                Point2D original = entry.getValue();
                node.setLayoutX(original.getX() + deltaX);
                node.setLayoutY(original.getY() + deltaY);
            }

            connectionManager.updateConnections();
            e.consume();
        }
    }

    public void setSelected(boolean selected) {
        background.setStroke(selected ? Color.YELLOW : Color.TRANSPARENT);
    }

    public static void clearSelection() {
        GraphNodeSelectionManager.getInstance().clear();
    }

    public static void deleteSelectedNodes() {
        var selectedNodes = GraphNodeSelectionManager.getInstance().getSelectedNodes();

        for (GraphNode node : selectedNodes) {
            node.connectionManager.removeConnectionsForNode(node);
            node.wrapperPane.getChildren().remove(node);
        }

        GraphNodeSelectionManager.getInstance().clear();
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

    public static void deleteSelectedNodes(AnchorPane canvas, ConnectionManager connectionManager) {
        var selectedNodes = GraphNodeSelectionManager.getInstance().getSelectedNodes();

        for (GraphNode node : selectedNodes) {
            connectionManager.removeConnectionsForNode(node);
            canvas.getChildren().remove(node);
        }

        GraphNodeSelectionManager.getInstance().clear();
    }
}
