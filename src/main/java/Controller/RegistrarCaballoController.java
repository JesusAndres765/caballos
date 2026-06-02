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

    @FXML
    private void initialize() {
        String username = SessionManager.getInstance().getUsuarioActual().getUsername();
        adminLabel.setText("Administrador: " + username);
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

        if (caballoDAO.insert(caballo)) {
            FXMLLoader loader = SceneManager.cambiarEscena("menu-admin.fxml");
            if (loader != null) {
                MenuAdminController ctrl = loader.getController();
                ctrl.setMensaje("Caballo \"" + nombre + "\" registrado correctamente.");
            }
        } else {
            mensajeLabel.setText("Error al registrar el caballo. Intenta de nuevo.");
        }
    }

    @FXML
    private void handleVolver() {
        SceneManager.cambiarEscena("menu-admin.fxml");
    }
}