import Controller.util.CarreraService;
import Controller.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.setPrimaryStage(primaryStage);
        primaryStage.setTitle("Ta' QueArdeElEstablo");
        primaryStage.setMaximized(true);
        SceneManager.cambiarEscena("login.fxml");

        CarreraService.getInstancia().iniciar();
    }

    @Override
    public void stop() {
        CarreraService.getInstancia().detener();
    }

    public static void main(String[] args) {
        launch(args);
    }
}