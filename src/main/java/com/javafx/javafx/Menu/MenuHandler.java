package com.javafx.javafx.Menu;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.HashSet;
import java.util.Set;

public class MenuHandler {

    private final Set<KeyCode> requiredKeys;
    private final ContextMenu contextMenu;
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private final Node targetNode;
    private boolean isMouseOver = false;
    private double lastMouseX;
    private double lastMouseY;
    private boolean useRightClick;
    private boolean useKeys;

    private MenuHandler(Builder builder) {
        this.targetNode = builder.targetNode;
        this.requiredKeys = builder.requiredKeys;
        this.contextMenu = builder.contextMenu;
        this.useRightClick = builder.useRightClick;
        this.useKeys = builder.useKeys;
        setupListeners();
        handleFilters();
    }

    private void setupListeners() {
        // Key events
        targetNode.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        targetNode.addEventFilter(KeyEvent.KEY_RELEASED, this::handleKeyReleased);

        // Mouse enter/exit to track mouse presence
        targetNode.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> isMouseOver = true);
        targetNode.addEventFilter(MouseEvent.MOUSE_EXITED, e -> isMouseOver = false);
    }

    private void handleFilters() {
        // Track last mouse position relative to target node
        targetNode.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });

        if (useRightClick) {
            targetNode.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    if (!contextMenu.isShowing()) {
                        Point2D screenPoint = targetNode.localToScreen(e.getX(), e.getY());
                        contextMenu.show(targetNode, screenPoint.getX(), screenPoint.getY());
                    }
                    e.consume();
                }
            });
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        if (!useKeys) return;

        pressedKeys.add(event.getCode());

        if (isMouseOver && !requiredKeys.isEmpty() && pressedKeys.containsAll(requiredKeys)) {
            if (!contextMenu.isShowing()) {
                Point2D screenPoint = targetNode.localToScreen(lastMouseX, lastMouseY);
                if (screenPoint != null) {
                    contextMenu.show(targetNode, screenPoint.getX(), screenPoint.getY());
                }
            }
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        if (!useKeys) return;
        pressedKeys.remove(event.getCode());
    }

    public double getLastMouseX() {
        return lastMouseX;
    }

    public double getLastMouseY() {
        return lastMouseY;
    }

    public static class Builder {
        private final Node targetNode;
        private final Set<KeyCode> requiredKeys;
        private final ContextMenu contextMenu;
        private boolean useRightClick = false; // default
        private boolean useKeys = true;       // default

        public Builder(Node targetNode, Set<KeyCode> requiredKeys, ContextMenu contextMenu) {
            this.targetNode = targetNode;
            this.requiredKeys = requiredKeys;
            this.contextMenu = contextMenu;
        }

        public Builder useRightClick(boolean useRightClick) {
            this.useRightClick = useRightClick;
            return this;
        }

        public Builder useKeys(boolean useKeys) {
            this.useKeys = useKeys;
            return this;
        }

        public MenuHandler build() {
            return new MenuHandler(this);
        }
    }
}
