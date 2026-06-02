package Controller;

import Controller.util.PasswordUtil;
import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Usuario;
import Model.dao.UsuarioDAO;
import Model.enums.Rol;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegistrarAdminController {

    @FXML private Label         adminLabel;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         mensajeLabel;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void initialize() {
        adminLabel.setText("Administrador: " +
                SessionManager.getInstance().getUsuarioActual().getUsername());
    }

    @FXML
    private void handleRegistrar() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            mensajeLabel.setText("Completa todos los campos.");
            return;
        }

        if (usuarioDAO.existeUsername(username)) {
            mensajeLabel.setText("El nombre de usuario ya está en uso.");
            return;
        }

        Usuario nuevoAdmin = new Usuario();
        nuevoAdmin.setUsername(username);
        nuevoAdmin.setContrasena(PasswordUtil.hash(password));
        nuevoAdmin.setRol(Rol.ADMIN);
        nuevoAdmin.setSaldo(0.0);

        if (usuarioDAO.insert(nuevoAdmin)) {
            FXMLLoader loader = SceneManager.cambiarEscena("menu-admin.fxml");
            if (loader != null) {
                MenuAdminController ctrl = loader.getController();
                ctrl.setMensaje("Administrador \"" + username + "\" registrado correctamente.");
            }
        } else {
            mensajeLabel.setText("Error al registrar. Intenta de nuevo.");
        }
    }

    @FXML
    private void handleVolver() {
        SceneManager.cambiarEscena("menu-admin.fxml");
    }
}