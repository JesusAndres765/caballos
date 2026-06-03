package Controller;

import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Caballo;
import Model.dao.CaballoDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class RegistrarCaballoController {

    @FXML private Label     adminLabel;
    @FXML private TextField nombreField;
    @FXML private TextField numeroField;
    @FXML private Label     mensajeLabel;

    private final CaballoDAO caballoDAO = new CaballoDAO();
    private MenuAdminController menuController;

    @FXML
    private void initialize() {
        String username = SessionManager.getInstance().getUsuarioActual().getUsername();
        adminLabel.setText("Administrador: " + username);
    }

    public void setMenuController(MenuAdminController ctrl) {
        this.menuController = ctrl;
    }

    @FXML
    private void handleRegistrar() {
        String nombre = nombreField.getText().trim();
        String numeroTexto = numeroField.getText().trim();

        // Validación: campos vacíos
        if (nombre.isEmpty() || numeroTexto.isEmpty()) {
            mensajeLabel.setText("Completa todos los campos.");
            return;
        }

        // Validación: número es entero positivo
        int numero;
        try {
            numero = Integer.parseInt(numeroTexto);
        } catch (NumberFormatException e) {
            mensajeLabel.setText("El número debe ser un valor entero.");
            return;
        }
        if (numero <= 0) {
            mensajeLabel.setText("El número debe ser mayor a cero.");
            return;
        }

        // Validación: número único
        if (caballoDAO.existeNumero(numero)) {
            mensajeLabel.setText("Ya existe un caballo con el número " + numero + ".");
            return;
        }

        Caballo caballo = new Caballo();
        caballo.setNombre(nombre);
        caballo.setNumero(numero);

        menuController.mostrarMensajeEnInicio(
                "Caballo \"" + nombre + "\" registrado correctamente.");
        nombreField.clear();
        numeroField.clear();
    }

    @FXML
    private void handleVolver() {
        if (menuController != null) menuController.irAInicio();
    }
}