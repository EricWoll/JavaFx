package com.javafx.javafx.lib.Connectors;

import com.javafx.javafx.lib.DataHolders.ConnectionRecord;
import com.javafx.javafx.lib.GraphNode.GraphNode;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

import static java.awt.geom.Line2D.linesIntersect;

public class ConnectionManager {
    private final AnchorPane canvas;
    private  ConnectorPoint startPoint;
    private Line dragLine;
    private final List<ConnectionRecord> connections = new ArrayList<>();
    private boolean isRightDragging = false;
    private Line sweepLine;

    public ConnectionManager(AnchorPane canvas) {
        this.canvas = canvas;

        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
    }

    public void startConnection(ConnectorPoint from) {
        if (from == null) return;

        // Remove any existing dragLine from canvas
        if (dragLine != null) {
            canvas.getChildren().remove(dragLine);
            dragLine = null;
        }

        this.startPoint = from;

        dragLine = new Line();
        updateLineToConnector(dragLine, startPoint);
        dragLine.setStroke(Color.YELLOW);
        dragLine.setStrokeWidth(2);
        canvas.getChildren().add(dragLine);
    }

    private void handleMousePressed(MouseEvent e) {
        canvas.setCursor(Cursor.HAND);
        if (e.isSecondaryButtonDown()) {
            // Start right-drag deletion
            isRightDragging = true;
            sweepLine = new Line(e.getX(), e.getY(), e.getX(), e.getY());
            sweepLine.setStroke(Color.RED);
            sweepLine.setStrokeWidth(2);
            sweepLine.getStrokeDashArray().addAll(10.0, 5.0);
            canvas.getChildren().add(sweepLine);
            e.consume();
        } else {
            // Left-click: connection drag start (if over a connector)
            ConnectorPoint cp = findConnectorAt(e.getX(), e.getY());
            if (cp != null && cp.getType() == ConnectorPoint.Type.OUTPUT) {
                startConnection(cp); // Existing logic
                e.consume();
            }
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        if (isRightDragging && sweepLine != null) {
            sweepLine.setEndX(e.getX());
            sweepLine.setEndY(e.getY());
            e.consume();
        } else if (dragLine != null) {
            dragLine.setEndX(e.getX());
            dragLine.setEndY(e.getY());
            e.consume();
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (isRightDragging && sweepLine != null) {
            removeIntersectingConnections(sweepLine);
            canvas.getChildren().remove(sweepLine);
            sweepLine = null;
            isRightDragging = false;
            e.consume();
        } else if (dragLine != null) {
            ConnectorPoint target = findConnectorAt(e.getX(), e.getY());
            if (target != null && startPoint != null) {
                completeConnection(startPoint, target);
            }
            canvas.getChildren().remove(dragLine);
            dragLine = null;
            startPoint = null;
            e.consume();
        }
        canvas.setCursor(Cursor.DEFAULT);
    }

    private ConnectorPoint findConnectorAt(double x, double y) {
        for (var child : canvas.getChildren()) {
            if (child instanceof GraphNode graphNode) {
                for (var inner : graphNode.getChildren()) {
                    if (inner instanceof ConnectorPoint cp) {
                        var local = cp.sceneToLocal(canvas.localToScene(x, y));
                        if (cp.contains(local)) {
                            return cp;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void completeConnection(ConnectorPoint from, ConnectorPoint to) {

        if (from.getType() == to.getType()) return;

        GraphNode fromGraphNode = from.getType() == ConnectorPoint.Type.OUTPUT ? from.getParentNode() : to.getParentNode();
        GraphNode toGraphNode = to.getType() == ConnectorPoint.Type.INPUT ? to.getParentNode() : from.getParentNode();

        boolean connectionExists = connectionExists(connections, fromGraphNode, toGraphNode);

        if (connectionExists) return;

        long count = connections.stream()
                .filter(c -> c.to() == toGraphNode)
                .count();

        if (!toGraphNode.allowsMoreConnections((int) count)) return;

        Line line = new Line();
        line.setStroke(Color.LIGHTGRAY);
        line.setStrokeWidth(2);

        ConnectionRecord conn = new ConnectionRecord(fromGraphNode, toGraphNode, line);
        connections.add(conn);
        canvas.getChildren().addFirst(line);

        updateConnections();
    }

    public void removeConnectionsForNode(GraphNode node) {
        List<ConnectionRecord> toRemove = new ArrayList<>();

        for (ConnectionRecord c : connections) {
            if (c.from() == node || c.to() == node) {
                canvas.getChildren().remove(c.line());
                toRemove.add(c);
            }
        }

        connections.removeAll(toRemove);
    }

    public boolean connectionExists(List<ConnectionRecord> connections, GraphNode fromGraphNode, GraphNode toGraphNode) {
        return connections.stream()
                .anyMatch(c -> c.from() == fromGraphNode && c.to() == toGraphNode);
    }


    private void updateLineToConnector(Line line, ConnectorPoint connector) {
        Point2D center = getConnectorCenter(connector);
        line.setStartX(center.getX());
        line.setStartY(center.getY());
        line.setEndX(center.getX());
        line.setEndY(center.getY());
    }

    private Point2D getConnectorCenter(ConnectorPoint connector) {
        Point2D scenePt = connector.localToScene(connector.getWidth() / 2, connector.getHeight() / 2);
        return canvas.sceneToLocal(scenePt);
    }

    public void handleConnectorClick(ConnectorPoint clicked) {
        if (startPoint == null) {
            // No connection started yet — remember this as the first point
            startPoint = clicked;
            return;
        }

        // A start point was already selected — check types and direction
        if (startPoint == clicked || startPoint.getType() == clicked.getType()) {
            // Same point or same type (input→input or output→output), reset
            startPoint = null;
            return;
        }

        // Ensure correct direction: always connect from OUTPUT to INPUT
//        ConnectorPoint from = startPoint.getType() == ConnectorPoint.Type.OUTPUT ? startPoint : clicked;
//        ConnectorPoint to = startPoint.getType() == ConnectorPoint.Type.INPUT ? clicked : startPoint;

        ConnectorPoint from = startPoint;

        completeConnection(from, clicked);
        startPoint = null;
    }


    public void updateConnections() {
        for (ConnectionRecord c : connections) {
            var start = getConnectorCenter(c.from().getConnector(ConnectorPoint.Type.OUTPUT));
            var end = getConnectorCenter(c.to().getConnector(ConnectorPoint.Type.INPUT));

            c.line().setStartX(start.getX());
            c.line().setStartY(start.getY());
            c.line().setEndX(end.getX());
            c.line().setEndY(end.getY());
        }
    }

    private void removeIntersectingConnections(Line dragLine) {
        List<ConnectionRecord> toRemove = new ArrayList<>();

        for (ConnectionRecord c : connections) {
            Line connLine = c.line();
            if (linesIntersect(
                    dragLine.getStartX(), dragLine.getStartY(), dragLine.getEndX(), dragLine.getEndY(),
                    connLine.getStartX(), connLine.getStartY(), connLine.getEndX(), connLine.getEndY())) {
                toRemove.add(c);
                canvas.getChildren().remove(connLine);
            }
        }

        connections.removeAll(toRemove);
    }

}
