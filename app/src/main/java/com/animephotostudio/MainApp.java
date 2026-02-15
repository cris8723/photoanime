package com.animephotostudio;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("AnimePhoto Studio");
        primaryStage.setScene(new Scene(root, 1000, 700));
        try {
            primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/icon.png")));
        } catch (Exception ignored) {
            // icon is optional for dev
        }
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}