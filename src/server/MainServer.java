package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainServer extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/server/controller.fxml"));
        primaryStage.setTitle("Chat [server]");
        primaryStage.toFront();
        primaryStage.centerOnScreen();
        primaryStage.setScene(new Scene(root, 800, 400));
        primaryStage.show();
    }
}

