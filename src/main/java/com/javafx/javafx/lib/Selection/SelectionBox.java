package com.javafx.javafx.lib.Selection;

import com.javafx.javafx.lib.Connectors.ConnectorPoint;
import com.javafx.javafx.lib.GraphNode.GraphNode;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

public class SelectionBox {

    private final GraphNodeSelectionManager selectionManager = GraphNodeSelectionManager.getInstance();

    private final Rectangle selectionBox = new Rectangle();
    private Point2D dragOffset;
    private boolean dragging = false;
    private final AnchorPane anchorPane;

    // Transparent mouse catcher rectangle
    private final Rectangle mouseCatcher = new Rectangle();

    public SelectionBox(AnchorPane anchorPane) {
        this.anchorPane = anchorPane;
        setupSelectionBox(anchorPane);
    }

    private void setupSelectionBox(AnchorPane canvas) {
        // Setup the transparent mouse catcher rectangle
        mouseCatcher.setFill(Color.TRANSPARENT);
        mouseCatcher.widthProperty().bind(canvas.widthProperty());
        mouseCatcher.heightProperty().bind(canvas.heightProperty());

        // Add mouseCatcher and selectionBox to the selectionLayer
        canvas.getChildren().addAll(mouseCatcher, selectionBox);

        selectionBox.setStroke(Color.LIGHTGOLDENRODYELLOW);
        selectionBox.setStrokeWidth(1);
        selectionBox.setFill(Color.LIGHTGOLDENRODYELLOW.deriveColor(0, 1.2, 1, 0.3));
        selectionBox.setVisible(false);

        // Only handle if click NOT on GraphNode or ConnectorPoint
        mouseCatcher.setOnMousePressed(this::onMousePress);
        mouseCatcher.setOnMouseDragged(this::onMouseDrag);
        mouseCatcher.setOnMouseReleased(this::onMouseRelease);
    }

    private void onMousePress(MouseEvent e) {
        if (!e.isPrimaryButtonDown()) return;

        Node target = e.getPickResult().getIntersectedNode();
        while (target != null) {
            if (target instanceof GraphNode || target instanceof ConnectorPoint) {
                // Don't start box selection if clicked on node/connector
                return;
            }
            target = target.getParent();
        }

        selectionManager.clear();
        dragOffset = new Point2D(e.getX(), e.getY());
        selectionBox.setX(dragOffset.getX());
        selectionBox.setY(dragOffset.getY());
        selectionBox.setWidth(0);
        selectionBox.setHeight(0);
        selectionBox.setVisible(true);
        dragging = true;
        e.consume();
    }

    private void onMouseDrag(MouseEvent e) {
        if (!dragging || dragOffset == null) return;

        if (dragOffset.getX() < 10 || dragOffset.getY() < 10) return;

        anchorPane.setCursor(Cursor.HAND);

        double x = Math.min(e.getX(), dragOffset.getX());
        double y = Math.min(e.getY(), dragOffset.getY());
        double width = Math.abs(e.getX() - dragOffset.getX());
        double height = Math.abs(e.getY() - dragOffset.getY());

        selectionBox.setX(x);
        selectionBox.setY(y);
        selectionBox.setWidth(width);
        selectionBox.setHeight(height);
        e.consume();
    }

    private void onMouseRelease(MouseEvent e) {
        if (!dragging) return;

        if (selectionBox.getWidth() < 5 || selectionBox.getHeight() < 5) {
            cancelSelectionBox();
            return;
        }

        Bounds boxBounds = selectionBox.getBoundsInParent();

        List<GraphNode> selected = anchorPane.getChildren().stream()
                .filter(n -> n instanceof GraphNode)
                .map(n -> (GraphNode) n)
                .filter(node -> node.getBoundsInParent().intersects(boxBounds))
                .toList();

        selectionManager.selectMultiple(selected);

        anchorPane.setCursor(Cursor.DEFAULT);
        cancelSelectionBox();
        e.consume();
    }

    public void cancelSelectionBox() {
        dragging = false;
        selectionBox.setVisible(false);
        dragOffset = null;
    }
}
