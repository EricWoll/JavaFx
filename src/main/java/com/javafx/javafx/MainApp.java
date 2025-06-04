package com.javafx.javafx;

import com.javafx.javafx.Menu.MenuHandler;
import com.javafx.javafx.Nodes.Connectors.ConnectionManager;
import com.javafx.javafx.Nodes.Node;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.Set;


public class MainApp extends Application {

    private Pane canvas;
    private ConnectionManager connectionManager;

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting!!");
        canvas = new Pane();
        canvas.setPrefSize(800, 600);
        canvas.setStyle("-fx-background-color: #2b2b2b;");

        connectionManager = new ConnectionManager(canvas);

        // Optional button for testing
        Button addButton = new Button("Add Node");
        addButton.setLayoutX(10);  // position from left
        addButton.setLayoutY(10);  // position from top
        addButton.setOnAction(e -> addNode(100, 100));
        canvas.getChildren().add(addButton);

        mainCanvasMenus(canvas);

        Scene scene = new Scene(canvas);

        // Deselect on click off or ESC
        scene.setOnMousePressed(e -> Node.clearSelection());
        scene.setOnKeyPressed(e -> {
            if (Objects.requireNonNull(e.getCode()) == KeyCode.ESCAPE) {
                Node.clearSelection();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Node Editor");
        primaryStage.show();
    }

    private void addNode(double x, double y) {
        Rectangle background = new Rectangle(120, 60);
        background.setArcWidth(15);
        background.setArcHeight(15);
        background.setFill(Color.DARKSLATEBLUE);
        background.setStrokeWidth(2);
        background.setStroke(Color.TRANSPARENT);

        Node node = new Node("Node " + (canvas.getChildren().size()), 0, connectionManager, background);
        node.setLayoutX(x);
        node.setLayoutY(y);
        canvas.getChildren().add(node);
    }

    private void mainCanvasMenus(Pane targetNode) {
        MenuItem addNodeItem = new MenuItem("Add Node");
        ContextMenu menu = new ContextMenu(addNodeItem);
        menu.setAutoHide(true);

        MenuHandler handler = new MenuHandler.Builder(targetNode, Set.of(KeyCode.SHIFT, KeyCode.A), menu).build();

        addNodeItem.setOnAction(e -> {
            double x = handler.getLastMouseX();
            double y = handler.getLastMouseY();
            addNode(x, y);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
