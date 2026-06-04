package Controller;

import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Transaccion;
import Model.Usuario;
import Model.dao.TransaccionDAO;
import Model.dao.UsuarioDAO;
import Model.enums.TipoTransaccion;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class DepositarController {

    @FXML private Label     usuarioLabel;
    @FXML private Label     saldoLabel;
    @FXML private TextField cantidadField;
    @FXML private TextField tarjetaField;
    @FXML private TextField titularField;
    @FXML private TextField vigField;
    @FXML private TextField cvcField;
    @FXML private Label     mensajeLabel;

    private final UsuarioDAO     usuarioDAO     = new UsuarioDAO();
    private final TransaccionDAO transaccionDAO = new TransaccionDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstancia().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
    }

    @FXML
    private void handleConfirmar() {
        // Validación: campos vacíos
        if (cantidadField.getText().trim().isEmpty() ||
                tarjetaField.getText().trim().isEmpty()  ||
                titularField.getText().trim().isEmpty()  ||
                vigField.getText().trim().isEmpty()      ||
                cvcField.getText().trim().isEmpty()) {
            mensajeLabel.setText("Completa todos los campos.");
            return;
        }

        // Validación: monto
        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadField.getText().trim());
        } catch (NumberFormatException e) {
            mensajeLabel.setText("La cantidad debe ser un número válido.");
            return;
        }
        if (cantidad <= 0) {
            mensajeLabel.setText("La cantidad debe ser mayor a cero.");
            return;
        }

        Usuario usuario = SessionManager.getInstancia().getUsuarioActual();
        double nuevoSaldo = usuario.getSaldo() + cantidad;

        // Actualiza saldo en BD
        if (!usuarioDAO.updateSaldo(usuario.getIdUsuario(), nuevoSaldo)) {
            mensajeLabel.setText("Error al procesar el depósito.");
            return;
        }

        // Log de transacción
        Transaccion t = new Transaccion();
        t.setIdUsuario(usuario.getIdUsuario());
        t.setTipo(TipoTransaccion.DEPOSITO);
        t.setMonto(cantidad);
        t.setDescripcion("Depósito con tarjeta terminada en " +
                ultimosCuatro(tarjetaField.getText().trim()));
        transaccionDAO.insert(t);

        // Actualiza sesión en memoria
        SessionManager.getInstancia().refrescarSaldo(nuevoSaldo);
        saldoLabel.setText(String.format("Saldo: %.2f", nuevoSaldo));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Depósito exitoso");
        alert.setHeaderText(null);
        alert.setContentText(String.format(
                "Se depositaron %.2f correctamente.\nNuevo saldo: %.2f", cantidad, nuevoSaldo));
        alert.showAndWait();

        cerrarVentana();
    }

    @FXML
    private void handleVolver() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        SceneManager.cambiarEscena("dashboard-usuario.fxml");
    }

    // Devuelve los últimos 4 dígitos de un número de tarjeta para el log
    private String ultimosCuatro(String numero) {
        String limpio = numero.replaceAll("\\s+", "");
        return limpio.length() >= 4
                ? limpio.substring(limpio.length() - 4)
                : limpio;
    }
}