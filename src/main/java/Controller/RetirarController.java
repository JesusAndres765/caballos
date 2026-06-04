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

public class RetirarController {

    @FXML private Label     usuarioLabel;
    @FXML private Label     saldoLabel;
    @FXML private TextField cantidadField;
    @FXML private TextField cuentaField;
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
        String cantTexto  = cantidadField.getText().trim();
        String cuentaTexto = cuentaField.getText().trim();

        // Validación: campos vacíos
        if (cantTexto.isEmpty() || cuentaTexto.isEmpty()) {
            mensajeLabel.setText("Completa todos los campos.");
            return;
        }

        // Validación: monto
        double cantidad;
        try {
            cantidad = Double.parseDouble(cantTexto);
        } catch (NumberFormatException e) {
            mensajeLabel.setText("La cantidad debe ser un número válido.");
            return;
        }
        if (cantidad <= 0) {
            mensajeLabel.setText("La cantidad debe ser mayor a cero.");
            return;
        }

        // Regla de negocio: no se puede retirar más del saldo disponible
        Usuario usuario = SessionManager.getInstancia().getUsuarioActual();
        if (cantidad > usuario.getSaldo()) {
            mensajeLabel.setText(String.format(
                    "Saldo insuficiente. Disponible: %.2f", usuario.getSaldo()));
            return;
        }

        double nuevoSaldo = usuario.getSaldo() - cantidad;

        if (!usuarioDAO.updateSaldo(usuario.getIdUsuario(), nuevoSaldo)) {
            mensajeLabel.setText("Error al procesar el retiro.");
            return;
        }

        // Log de transacción
        Transaccion t = new Transaccion();
        t.setIdUsuario(usuario.getIdUsuario());
        t.setTipo(TipoTransaccion.RETIRO);
        t.setMonto(cantidad);
        t.setDescripcion("Retiro a cuenta: " + cuentaTexto);
        transaccionDAO.insert(t);

        SessionManager.getInstancia().refrescarSaldo(nuevoSaldo);
        saldoLabel.setText(String.format("Saldo: %.2f", nuevoSaldo));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Retiro exitoso");
        alert.setHeaderText(null);
        alert.setContentText(String.format(
                "Se retiraron %.2f correctamente.\nNuevo saldo: %.2f", cantidad, nuevoSaldo));
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
}