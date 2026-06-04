package Controller.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.Region;

import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    private SceneManager() {}

    // Se llama una sola vez desde MainApp al arrancar la aplicacion
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static FXMLLoader cambiarEscena(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(
                            "/com/taqueardeelestablo/view/" + fxmlFile)
            );
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
            return loader;
        } catch (IOException e) {
            System.err.println("SceneManager.cambiarEscena [" + fxmlFile + "]: " + e.getMessage());
            return null;
        }
    }

    // Abre el FXML en una nueva ventana independiente
    public static FXMLLoader abrirVentana(String fxmlFile, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource("/com/taqueardeelestablo/view/" + fxmlFile));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(root));
            stage.show();
            return loader;
        } catch (IOException e) {
            System.err.println("SceneManager.abrirVentana [" + fxmlFile + "]: " + e.getMessage());
            return null;
        }
    }

    public static FXMLLoader abrirModal(String fxmlFile, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource("/com/taqueardeelestablo/view/" + fxmlFile));
            Parent root = loader.load();

            double w = (root instanceof Region r && r.getPrefWidth()  > 0) ? r.getPrefWidth()  : 400;
            double h = (root instanceof Region r && r.getPrefHeight() > 0) ? r.getPrefHeight() : 300;

            Stage modal = new Stage();
            modal.setTitle(titulo);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(primaryStage);
            modal.setScene(new Scene(root, w, h));
            modal.setResizable(false);
            modal.centerOnScreen();
            modal.showAndWait();
            return loader;

        } catch (IOException e) {
            System.err.println("SceneManager.abrirModal [" + fxmlFile + "]: " + e.getMessage());
            return null;
        }
    }
}