package Controller;

import Controller.util.SessionManager;
import Model.Apuesta;
import Model.Caballo;
import Model.Carrera;
import Model.Transaccion;
import Model.Usuario;
import Model.dao.ApuestaDAO;
import Model.dao.TransaccionDAO;
import Model.dao.UsuarioDAO;
import Model.enums.ResultadoApuesta;
import Model.enums.TipoTransaccion;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;

public class ModalApostarController {

    @FXML private Label              porcentajeLabel;
    @FXML private TableView<Caballo> caballosTabla;
    @FXML private ComboBox<Caballo>  caballoCombo;
    @FXML private TextField          apuestaField;
    @FXML private Label              mensajeLabel;
    @FXML private Button             confirmBtn;
    @FXML private Button             cancelBtn;

    private final ApuestaDAO     apuestaDAO     = new ApuestaDAO();
    private final UsuarioDAO     usuarioDAO     = new UsuarioDAO();
    private final TransaccionDAO transaccionDAO = new TransaccionDAO();

    private Carrera        carrera;
    private List<Caballo>  caballos;

    @FXML
    private void initialize() {
        // Columnas de la tabla definidas aquí para no duplicarlas en FXML
        TableColumn<Caballo, String> nombreCol = new TableColumn<>("Nombre");
        nombreCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Caballo, Number> numeroCol = new TableColumn<>("Numero");
        numeroCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getNumero()));

        TableColumn<Caballo, Number> corridasCol = new TableColumn<>("Corridas");
        corridasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasCorridas()));

        TableColumn<Caballo, Number> ganadasCol = new TableColumn<>("Ganadas");
        ganadasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasGanadas()));

        caballosTabla.getColumns().addAll(nombreCol, numeroCol, corridasCol, ganadasCol);
        caballosTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // Llamado desde DashboardUsuarioController ANTES de showAndWait
    public void setDatos(Carrera carrera, List<Caballo> caballos) {
        this.carrera  = carrera;
        this.caballos = caballos;

        porcentajeLabel.setText(
                String.format("Multiplicador: x%d   %%: %.2f",
                        carrera.getNumCaballos(), 100.0 / carrera.getNumCaballos())
        );

        caballosTabla.setItems(FXCollections.observableArrayList(caballos));

        // ComboBox muestra "Nombre - No. N"
        caballoCombo.setItems(FXCollections.observableArrayList(caballos));
        caballoCombo.setConverter(new StringConverter<Caballo>() {
            @Override public String toString(Caballo c) {
                return c == null ? "" : c.getNombre() + " - No. " + c.getNumero();
            }
            @Override public Caballo fromString(String s) { return null; }
        });
        if (!caballos.isEmpty()) caballoCombo.setValue(caballos.get(0));
    }

    @FXML
    private void handleConfirmar() {
        Caballo seleccionado = caballoCombo.getValue();
        String  montoTexto   = apuestaField.getText().trim();

        if (seleccionado == null) {
            mensajeLabel.setText("Selecciona un caballo.");
            return;
        }
        if (montoTexto.isEmpty()) {
            mensajeLabel.setText("Ingresa el monto de la apuesta.");
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoTexto);
        } catch (NumberFormatException e) {
            mensajeLabel.setText("El monto debe ser un número válido.");
            return;
        }
        if (monto <= 0) {
            mensajeLabel.setText("El monto debe ser mayor a cero.");
            return;
        }

        Usuario usuario = SessionManager.getInstance().getUsuarioActual();
        if (monto > usuario.getSaldo()) {
            mensajeLabel.setText("Saldo insuficiente. Tienes: " +
                    String.format("%.2f", usuario.getSaldo()));
            return;
        }

        // Crear apuesta
        Apuesta apuesta = new Apuesta();
        apuesta.setIdUsuario(usuario.getIdUsuario());
        apuesta.setIdCarrera(carrera.getIdCarrera());
        apuesta.setIdCaballo(seleccionado.getIdCaballo());
        apuesta.setMonto(monto);
        apuesta.setMultiplicador(carrera.getNumCaballos());
        apuesta.setResultado(ResultadoApuesta.PENDIENTE);
        apuesta.setCobro(0.0);

        if (!apuestaDAO.insert(apuesta)) {
            mensajeLabel.setText("Error al registrar la apuesta.");
            return;
        }

        // Descontar saldo
        double nuevoSaldo = usuario.getSaldo() - monto;
        usuarioDAO.updateSaldo(usuario.getIdUsuario(), nuevoSaldo);
        SessionManager.getInstance().refrescarSaldo(nuevoSaldo);

        // Log de transacción
        Transaccion t = new Transaccion();
        t.setIdUsuario(usuario.getIdUsuario());
        t.setTipo(TipoTransaccion.APUESTA);
        t.setMonto(monto);
        t.setDescripcion("Apuesta carrera #" + carrera.getIdCarrera() +
                " — Caballo: " + seleccionado.getNombre());
        transaccionDAO.insert(t);

        cerrarVentana();
    }

    @FXML
    private void handleCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        ((Stage) confirmBtn.getScene().getWindow()).close();
    }
}