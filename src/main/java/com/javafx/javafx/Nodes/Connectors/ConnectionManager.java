package com.javafx.javafx.Nodes.Connectors;

import com.javafx.javafx.Nodes.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

import static java.awt.geom.Line2D.linesIntersect;

public class ConnectionManager {
    private final Pane canvas;
    private ConnectorPoint startPoint;
    private Line dragLine;
    private final List<Connection> connections = new ArrayList<>();
    private boolean isRightDragging = false;
    private Line sweepLine;

    public ConnectionManager(Pane canvas) {
        this.canvas = canvas;

        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
    }

    public void startConnection(ConnectorPoint from) {
        if (from == null) return;

        this.startPoint = from;

        dragLine = new Line();
        updateLineToConnector(dragLine, startPoint);
        dragLine.setStroke(Color.YELLOW);
        dragLine.setStrokeWidth(2);
        canvas.getChildren().add(dragLine);
    }

    private void handleMousePressed(MouseEvent e) {
        if (e.isSecondaryButtonDown()) {
            // Start right-drag deletion
            isRightDragging = true;
            sweepLine = new Line(e.getX(), e.getY(), e.getX(), e.getY());
            sweepLine.setStroke(Color.RED);
            sweepLine.setStrokeWidth(2);
            sweepLine.getStrokeDashArray().addAll(10.0, 5.0);
            canvas.getChildren().add(sweepLine);
        } else {
            // Left-click: connection drag start (if over a connector)
            ConnectorPoint cp = findConnectorAt(e.getX(), e.getY());
            if (cp != null && cp.getType() == ConnectorPoint.Type.OUTPUT) {
                startConnection(cp); // Existing logic
            }
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        if (isRightDragging && sweepLine != null) {
            sweepLine.setEndX(e.getX());
            sweepLine.setEndY(e.getY());
        } else if (dragLine != null) {
            dragLine.setEndX(e.getX());
            dragLine.setEndY(e.getY());
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (isRightDragging && sweepLine != null) {
            removeIntersectingConnections(sweepLine);
            canvas.getChildren().remove(sweepLine);
            sweepLine = null;
            isRightDragging = false;
        } else if (dragLine != null && startPoint != null) {
            ConnectorPoint target = findConnectorAt(e.getX(), e.getY());
            if (target != null) {
                completeConnection(startPoint, target);
            }
            canvas.getChildren().remove(dragLine);
            dragLine = null;
            startPoint = null;
        }
    }

    private ConnectorPoint findConnectorAt(double x, double y) {
        for (var child : canvas.getChildren()) {
            if (child instanceof Node node) {
                for (var inner : node.getChildren()) {
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
        Node fromNode = from.getParentNode();
        Node toNode = to.getParentNode();

        long count = connections.stream()
                .filter(c -> c.to == toNode)
                .count();

        if (!toNode.allowsMoreConnections((int) count)) return;

        Line line = new Line();
        line.setStroke(Color.LIGHTGRAY);
        line.setStrokeWidth(2);

        Connection conn = new Connection(fromNode, toNode, line);
        connections.add(conn);
        canvas.getChildren().add(0, line);

        updateConnections();
    }


    private void updateLineToConnector(Line line, ConnectorPoint connector) {
        var center = getConnectorCenter(connector);
        line.setStartX(center.getX());
        line.setStartY(center.getY());
        line.setEndX(center.getX());
        line.setEndY(center.getY());
    }

    private javafx.geometry.Point2D getConnectorCenter(ConnectorPoint connector) {
        var scenePt = connector.localToScene(connector.getWidth() / 2, connector.getHeight() / 2);
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
        ConnectorPoint from = startPoint.getType() == ConnectorPoint.Type.OUTPUT ? startPoint : clicked;
        ConnectorPoint to = startPoint.getType() == ConnectorPoint.Type.INPUT ? startPoint : clicked;

        completeConnection(from, to);
        startPoint = null;
    }


    public void updateConnections() {
        for (Connection c : connections) {
            var start = getConnectorCenter(c.from.getConnector(ConnectorPoint.Type.OUTPUT));
            var end = getConnectorCenter(c.to.getConnector(ConnectorPoint.Type.INPUT));

            c.line.setStartX(start.getX());
            c.line.setStartY(start.getY());
            c.line.setEndX(end.getX());
            c.line.setEndY(end.getY());
        }
    }

    private void removeIntersectingConnections(Line dragLine) {
        List<Connection> toRemove = new ArrayList<>();

        for (Connection c : connections) {
            Line connLine = c.line;
            if (linesIntersect(
                    dragLine.getStartX(), dragLine.getStartY(), dragLine.getEndX(), dragLine.getEndY(),
                    connLine.getStartX(), connLine.getStartY(), connLine.getEndX(), connLine.getEndY())) {
                toRemove.add(c);
                canvas.getChildren().remove(connLine);
            }
        }

        connections.removeAll(toRemove);
    }

    private static class Connection {
        final Node from;
        final Node to;
        final Line line;

        Connection(Node from, Node to, Line line) {
            this.from = from;
            this.to = to;
            this.line = line;
        }
    }

}
