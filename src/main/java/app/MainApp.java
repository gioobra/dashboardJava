package app;

import controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private MainController mainController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();

            mainController = loader.getController();

            Scene scene = new Scene(root, 1280, 720);

            String cssPath = getClass().getResource("/view/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            primaryStage.setTitle("Task Manager");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(event -> {
                if (mainController != null) {
                    mainController.shutdown();
                }
            });

            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
