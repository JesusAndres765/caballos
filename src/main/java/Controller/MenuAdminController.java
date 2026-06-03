package Controller;

import Controller.util.SceneManager;
import Controller.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MenuAdminController {

    @FXML private Label adminLabel;
    @FXML private Label mensajeLabel;

    @FXML
    private void initialize() {
        String username = SessionManager.getInstance().getUsuarioActual().getUsername();
        adminLabel.setText("Administrador: " + username);
    }

    public void setMensaje(String mensaje) {
        mensajeLabel.setText(mensaje);
    }

    @FXML
    private void handleCrearCarrera() {
        mensajeLabel.setText("");
        SceneManager.cambiarEscena("crear-carrera.fxml");
    }

    @FXML
    private void handleRegistrarCaballo() {
        mensajeLabel.setText("");
        SceneManager.cambiarEscena("registrar-caballo.fxml");
    }

    @FXML
    private void handleRegistrarAdmin() {
        mensajeLabel.setText("");
        SceneManager.cambiarEscena("registrar-admin.fxml");
    }

    @FXML
    private void handleVerCaballos(){
        mensajeLabel.setText("");
        SceneManager.cambiarEscena("caballos-registrados.fxml");
    }

    @FXML
    private void handleTerminarSesion() {
        SessionManager.getInstance().cerrarSesion();
        SceneManager.cambiarEscena("login.fxml");
    }
}