import Controller.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Registra el stage principal en SceneManager para que
        // cualquier controlador pueda navegar sin tener referencia al Stage
        SceneManager.setPrimaryStage(primaryStage);

        primaryStage.setTitle("Ta' QueArdeElEstablo");
        primaryStage.setMaximized(true);

        // Primera pantalla que ve el usuario al abrir la app
        SceneManager.cambiarEscena("login.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}