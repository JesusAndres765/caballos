package Controller;

import Model.Caballo;
import Model.dao.CaballoDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ActualizarCaballoController {

    @FXML private TextField nombreField;
    @FXML private TextField numeroField;
    @FXML private Label     mensajeLabel;
    @FXML private Button    actualizarBtn;

    private Caballo          caballoOriginal;
    private final CaballoDAO caballoDAO = new CaballoDAO();

    // Llamado desde CaballosRegistradosController ANTES de showAndWait
    public void setCaballo(Caballo caballo) {
        this.caballoOriginal = caballo;
        nombreField.setText(caballo.getNombre());
        numeroField.setText(String.valueOf(caballo.getNumero()));
    }

    @FXML
    private void handleActualizar() {
        String nuevoNombre = nombreField.getText().trim();
        String numeroTexto = numeroField.getText().trim();

        // Validación: campos vacíos
        if (nuevoNombre.isEmpty() || numeroTexto.isEmpty()) {
            mensajeLabel.setText("Completa todos los campos.");
            return;
        }

        // Validación: número entero positivo
        int nuevoNumero;
        try {
            nuevoNumero = Integer.parseInt(numeroTexto);
        } catch (NumberFormatException e) {
            mensajeLabel.setText("El número debe ser un valor entero.");
            return;
        }
        if (nuevoNumero <= 0) {
            mensajeLabel.setText("El número debe ser mayor a cero.");
            return;
        }

        // Validación: unicidad de número solo si cambió
        if (nuevoNumero != caballoOriginal.getNumero()) {
            if (caballoDAO.existeNumero(nuevoNumero)) {
                mensajeLabel.setText("Ya existe un caballo con el número " + nuevoNumero + ".");
                return;
            }
        }

        // Aplica los cambios al objeto y persiste
        caballoOriginal.setNombre(nuevoNombre);
        caballoOriginal.setNumero(nuevoNumero);

        if (caballoDAO.update(caballoOriginal)) {
            cerrarVentana();
        } else {
            mensajeLabel.setText("Error al actualizar. Intenta de nuevo.");
        }
    }

    @FXML
    private void handleCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        ((Stage) actualizarBtn.getScene().getWindow()).close();
    }
}