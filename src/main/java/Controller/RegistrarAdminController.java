package Controller;

import Controller.util.PasswordUtil;
import Controller.util.SessionManager;
import Model.Usuario;
import Model.dao.UsuarioDAO;
import Model.enums.Rol;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegistrarAdminController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label mensajeLabel;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private MenuAdminController menuController;

    @FXML
    private void initialize() {}

    public void setMenuController(MenuAdminController ctrl) {
        this.menuController = ctrl;
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

        if (!usuarioDAO.insert(nuevoAdmin)) {
            mensajeLabel.setText("Error al registrar el administrador.");
            return;
        }

        usernameField.clear();
        passwordField.clear();
        mensajeLabel.setText("");
        menuController.mostrarMensajeEnInicio("Administrador \"" + username + "\" registrado correctamente.");
    }

    @FXML
    private void handleVolver() {
        if (menuController != null) menuController.irAInicio();
    }
}