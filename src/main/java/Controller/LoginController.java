package Controller;

import Controller.util.PasswordUtil;
import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Usuario;
import Model.dao.UsuarioDAO;
import Model.enums.Rol;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label        mensajeLabel;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void handleIniciarSesion() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            mensajeLabel.setText("Completa todos los campos.");
            return;
        }

        Usuario usuario = usuarioDAO.findByUsername(username);

        if (usuario == null) {
            mensajeLabel.setText("Usuario no encontrado.");
            return;
        }

        if (!PasswordUtil.verificar(password, usuario.getContrasena())) {
            mensajeLabel.setText("Contraseña incorrecta.");
            return;
        }

        SessionManager.getInstancia().iniciarSesion(usuario);

        if (usuario.getRol() == Rol.ADMIN) {
            SceneManager.cambiarEscena("menu-admin.fxml");
        } else {
            SceneManager.cambiarEscena("dashboard-usuario.fxml");
        }
    }

    @FXML
    private void handleRegistrarse() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            mensajeLabel.setText("Completa todos los campos.");
            return;
        }

        if (usuarioDAO.existeUsername(username)) {
            mensajeLabel.setText("Ese nombre de usuario ya está en uso.");
            return;
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setContrasena(PasswordUtil.hash(password));
        nuevo.setRol(Rol.USUARIO);
        nuevo.setSaldo(0.0);

        if (usuarioDAO.insert(nuevo)) {
            SessionManager.getInstancia().iniciarSesion(nuevo);
            SceneManager.cambiarEscena("dashboard-usuario.fxml");
        } else {
            mensajeLabel.setText("Error al registrar. Intenta de nuevo.");
        }
    }
}