package Controller;

import Model.Caballo;
import Model.dao.CaballoDAO;
import Controller.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class RegistrarCaballoController {

    // adminLabel eliminado — el menú padre ya muestra el nombre del admin
    @FXML private TextField nombreField;
    @FXML private TextField numeroField;
    @FXML private Label     mensajeLabel;

    private final CaballoDAO caballoDAO = new CaballoDAO();
    private MenuAdminController menuController;

    @FXML
    private void initialize() {
        // Sin adminLabel: nada que inicializar aquí por ahora
    }

    public void setMenuController(MenuAdminController ctrl) {
        this.menuController = ctrl;
    }

    @FXML
    private void handleRegistrar() {
        String nombre      = nombreField.getText().trim();
        String numeroTexto = numeroField.getText().trim();

        if (nombre.isEmpty() || numeroTexto.isEmpty()) {
            mensajeLabel.setText("Completa todos los campos.");
            return;
        }

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
        if (caballoDAO.existeNumero(numero)) {
            mensajeLabel.setText("Ya existe un caballo con el número " + numero + ".");
            return;
        }

        Caballo caballo = new Caballo();
        caballo.setNombre(nombre);
        caballo.setNumero(numero);

        // ← Bug 2 corregido: ahora SÍ se guarda en BD
        if (!caballoDAO.insert(caballo)) {
            mensajeLabel.setText("Error al guardar el caballo en la base de datos.");
            return;
        }

        nombreField.clear();
        numeroField.clear();
        mensajeLabel.setText("");
        menuController.mostrarMensajeEnInicio(
                "Caballo \"" + nombre + "\" registrado correctamente.");
    }

    @FXML
    private void handleVolver() {
        if (menuController != null) menuController.irAInicio();
    }
}