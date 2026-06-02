package Controller.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    private SceneManager() {}

    // Se llama una sola vez desde MainApp al arrancar la aplicación
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // Reemplaza la escena en la ventana principal.
    // Devuelve el FXMLLoader para que el llamador pueda obtener el controlador
    // y pasarle datos si lo necesita:
    //
    //   FXMLLoader loader = SceneManager.cambiarEscena("ver-carrera.fxml");
    //   VerCarreraController ctrl = loader.getController();
    //   ctrl.setCarrera(carrera);
    //
    // Si no necesitas datos, simplemente ignora el valor de retorno:
    //   SceneManager.cambiarEscena("login.fxml");
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

    // Abre el FXML en una nueva ventana independiente.
    // Útil para: VerCarrera, Resultados, Historial, Depositar, Retirar.
    public static FXMLLoader abrirVentana(String fxmlFile, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(
                            "/com/taqueardeelestablo/view/" + fxmlFile)
            );
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

    // Abre el FXML como ventana modal: bloquea la ventana principal
    // hasta que el usuario la cierre.
    // Útil para: ModalApostar, ApuestasActivas.
    public static FXMLLoader abrirModal(String fxmlFile, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(
                            "/com/taqueardeelestablo/view/" + fxmlFile)
            );
            Parent root = loader.load();
            Stage modal = new Stage();
            modal.setTitle(titulo);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(primaryStage);
            modal.setScene(new Scene(root));
            modal.showAndWait(); // bloquea hasta que se cierre
            return loader;
        } catch (IOException e) {
            System.err.println("SceneManager.abrirModal [" + fxmlFile + "]: " + e.getMessage());
            return null;
        }
    }
}