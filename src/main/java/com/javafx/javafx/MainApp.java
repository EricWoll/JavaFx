package com.javafx.javafx;

import com.javafx.javafx.lib.Selection.SelectionBox;
import com.javafx.javafx.lib.Menu.MenuHandler;
import com.javafx.javafx.lib.GraphNode.GraphNode;
import com.javafx.javafx.lib.Connectors.ConnectionManager;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.Set;

public class MainApp extends Application {

    private AnchorPane canvas;
    private AnchorPane contentGroup; // Changed from Group to AnchorPane
    private ConnectionManager connectionManager;

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting!!");

        canvas = new AnchorPane();
        canvas.setPrefSize(800, 600);
        canvas.setStyle("-fx-background-color: #2b2b2b;");

        contentGroup = new AnchorPane();
        // Bind contentGroup size to fill canvas
        contentGroup.prefWidthProperty().bind(canvas.widthProperty());
        contentGroup.prefHeightProperty().bind(canvas.heightProperty());
        contentGroup.setPickOnBounds(false);

        AnchorPane selectionLayer = new AnchorPane();
        selectionLayer.prefWidthProperty().bind(canvas.widthProperty());
        selectionLayer.prefHeightProperty().bind(canvas.heightProperty());
        selectionLayer.setPickOnBounds(false); // Allow mouse events through empty space if needed
        selectionLayer.setMouseTransparent(false); // Adjust depending on your use case

        canvas.getChildren().addAll(contentGroup, selectionLayer);

        connectionManager = new ConnectionManager(contentGroup);

        // Panning variables
        final double[] dragStart = new double[2];

        canvas.setOnMousePressed(e -> {
            if (e.isMiddleButtonDown()) {
                canvas.setCursor(Cursor.CLOSED_HAND);
                dragStart[0] = e.getSceneX();
                dragStart[1] = e.getSceneY();
                e.consume();
            }
            if (!e.isMiddleButtonDown()) {
                GraphNode.clearSelection();
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (e.getButton().name().equals("MIDDLE")) {
                canvas.setCursor(Cursor.DEFAULT); // Reset to normal
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (e.isMiddleButtonDown()) {
                double deltaX = e.getSceneX() - dragStart[0];
                double deltaY = e.getSceneY() - dragStart[1];

                contentGroup.setTranslateX(contentGroup.getTranslateX() + deltaX);
                contentGroup.setTranslateY(contentGroup.getTranslateY() + deltaY);

                dragStart[0] = e.getSceneX();
                dragStart[1] = e.getSceneY();
                e.consume();
            }
        });

        canvas.setOnScroll(this::onMouseScroll);


        // Optional button for testing
        Button addButton = new Button("Add Node");
        addButton.setLayoutX(10);
        addButton.setLayoutY(10);
        addButton.setOnAction(e -> addNode(100, 100));
        canvas.getChildren().add(addButton);

        mainCanvasMenus(canvas);

        // Pass contentGroup as wrapperPane to GraphNode for coordinate conversions
        new SelectionBox(contentGroup);

        Scene scene = new Scene(canvas);

        scene.setOnKeyPressed(e -> {
            if (Objects.requireNonNull(e.getCode()) == KeyCode.ESCAPE) {
                GraphNode.clearSelection();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Node Editor");
        primaryStage.show();
    }

    private void onMouseScroll(ScrollEvent event) {
        double zoomFactor = 1.05;
        double oldScale = contentGroup.getScaleX();
        double delta = event.getDeltaY();

        double scale = (delta > 0) ? oldScale * zoomFactor : oldScale / zoomFactor;
        scale = Math.min(Math.max(scale, 0.5), 2); // clamp between 0.5 and 2

        if (scale == oldScale) return; // no change

        // Get the mouse position in contentGroup's coordinates
        Point2D mouseSceneCoords = new Point2D(event.getSceneX(), event.getSceneY());
        Point2D mouseInContent = contentGroup.sceneToLocal(mouseSceneCoords);

        // Apply scale
        contentGroup.setScaleX(scale);
        contentGroup.setScaleY(scale);

        // After scaling, convert back to scene coords to see where content moved
        Point2D newMouseSceneCoords = contentGroup.localToScene(mouseInContent);

        // Compute the delta in scene space and apply it to translation
        double dx = mouseSceneCoords.getX() - newMouseSceneCoords.getX();
        double dy = mouseSceneCoords.getY() - newMouseSceneCoords.getY();

        contentGroup.setTranslateX(contentGroup.getTranslateX() + dx);
        contentGroup.setTranslateY(contentGroup.getTranslateY() + dy);

        event.consume();
    }

    private void addNode(double x, double y) {
        Rectangle background = new Rectangle(120, 60);
        background.setArcWidth(15);
        background.setArcHeight(15);
        background.setFill(Color.DARKSLATEBLUE);
        background.setStrokeWidth(2);
        background.setStroke(Color.TRANSPARENT);

        GraphNode graphNode = new GraphNode(
                "Node " + (contentGroup.getChildren().size()),
                0,
                connectionManager,
                background,
                contentGroup, // contentGroup is AnchorPane now
                contentGroup  // pass same AnchorPane as wrapperPane for coordinate conversions
        );
        graphNode.setLayoutX(x);
        graphNode.setLayoutY(y);
        contentGroup.getChildren().add(graphNode);
    }

    private void mainCanvasMenus(AnchorPane canvas) {
        MenuItem addNodeItem = new MenuItem("Add Node");
        ContextMenu menu = new ContextMenu(addNodeItem);
        menu.setAutoHide(true);

        MenuHandler handler = new MenuHandler.Builder(canvas, menu)
                .setPressedKeys(Set.of(KeyCode.SHIFT, KeyCode.A))
                .build();

        addNodeItem.setOnAction(e -> {
            double sceneX = handler.getLastMouseX();
            double sceneY = handler.getLastMouseY();
            Point2D localCoords = contentGroup.sceneToLocal(sceneX, sceneY);

            addNode(localCoords.getX(), localCoords.getY());
        });

        canvas.setOnMouseClicked(e -> menu.hide());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
