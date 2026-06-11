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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ModalApostarController {
    @FXML private Label multiplicadorLabel;
    @FXML private Label porcentajeLabel;
    @FXML private Label cierreLabel;
    @FXML private TableView<Caballo> caballosTabla;
    @FXML private ComboBox<Caballo> caballoCombo;
    @FXML private TextField apuestaField;
    @FXML private Label mensajeLabel;
    @FXML private Button confirmBtn;
    @FXML private Button cancelBtn;

    private final ApuestaDAO apuestaDAO = new ApuestaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TransaccionDAO transaccionDAO = new TransaccionDAO();

    private Carrera carrera;
    private List<Caballo> caballos;
    private Timeline cierreTimeline;

    private static final int SEGUNDOS_ANTES_CIERRE = 30;

    @FXML
    private void initialize() {
        TableColumn<Caballo, String> nombreCol = new TableColumn<>("Nombre");
        nombreCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Caballo, Number> numeroCol = new TableColumn<>("Numero");
        numeroCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getNumero()));

        TableColumn<Caballo, Number> corridasCol = new TableColumn<>("Carreras Corridas");
        corridasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasCorridas()));

        TableColumn<Caballo, Number> ganadasCol = new TableColumn<>("Carreras Ganadas");
        ganadasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasGanadas()));

        caballosTabla.getColumns().addAll(nombreCol, numeroCol, corridasCol, ganadasCol);
        caballosTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void setDatos(Carrera carrera, List<Caballo> caballos) {
        this.carrera  = carrera;
        this.caballos = caballos;

        multiplicadorLabel.setText("Multiplicador: x" + carrera.getNumCaballos());
        porcentajeLabel.setText(String.format("%%: %.2f", 100.0 / carrera.getNumCaballos()));

        caballosTabla.setItems(FXCollections.observableArrayList(caballos));
        configurarComboBox();

        // Calcula los segundos que faltan para el cierre de apuestas
        LocalDateTime fechaCreacion = carrera.getFechaCreacion();

        if (fechaCreacion == null) {
            cierreLabel.setText("Las Apuestas Cierran en: calculando...");
            return;
        }

        LocalDateTime inicioCarrera  = fechaCreacion.plusMinutes(carrera.getTiempoGatera());
        LocalDateTime cierreApuestas = inicioCarrera.minusSeconds(SEGUNDOS_ANTES_CIERRE);
        long segundosRestantes = ChronoUnit.SECONDS.between(LocalDateTime.now(), cierreApuestas);

        if (segundosRestantes <= 0) {
            bloquearApuestas("Las apuestas para esta carrera ya están cerradas.");
        } else {
            iniciarCuentaRegresiva(segundosRestantes);
        }
    }

    // Cuenta regresiva que muestra cuanto tiempo queda para apostar
    private void iniciarCuentaRegresiva(long segundosIniciales) {
        long[] segundos = { segundosIniciales };
        cierreLabel.setText("Las Apuestas Cierran en: " + formatearTiempo(segundos[0]));

        cierreTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundos[0]--;
            if (segundos[0] <= 0) {
                cierreTimeline.stop();
                bloquearApuestas("El tiempo de apuestas ha cerrado.");
            } else {
                cierreLabel.setText("Las Apuestas Cierran en: " + formatearTiempo(segundos[0]));
            }
        }));
        cierreTimeline.setCycleCount(Timeline.INDEFINITE);
        cierreTimeline.play();
    }

    // Deshabilita todos los controles de apuesta cuando se acaba el tiempo
    private void bloquearApuestas(String motivo) {
        cierreLabel.setText("Apuestas cerradas");
        caballoCombo.setDisable(true);
        apuestaField.setDisable(true);
        confirmBtn.setDisable(true);
        mensajeLabel.setText(motivo);
    }

    private String formatearTiempo(long seg) {
        return String.format("%d:%02d", seg / 60, seg % 60);
    }

    private void configurarComboBox() {
        caballoCombo.setItems(FXCollections.observableArrayList(caballos));
        caballoCombo.setConverter(new StringConverter<Caballo>() {
            @Override
            public String toString(Caballo c) {
                return c == null ? "" : c.getNombre() + " - No. " + c.getNumero();
            }
            @Override
            public Caballo fromString(String s) { return null; }
        });
        if (!caballos.isEmpty()) {
            caballoCombo.setValue(caballos.get(0));
        }
    }

    @FXML
    private void handleConfirmar() {
        if (confirmBtn.isDisabled()) return;

        Caballo seleccionado = caballoCombo.getValue();
        String  montoTexto = apuestaField.getText().trim();

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

        Usuario usuario = SessionManager.getInstancia().getUsuarioActual();
        if (monto > usuario.getSaldo()) {
            mensajeLabel.setText("Saldo insuficiente. Tienes: " + String.format("%.2f", usuario.getSaldo()));
            return;
        }

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

        // Descuenta el saldo y registra la transaccion
        double nuevoSaldo = usuario.getSaldo() - monto;
        usuarioDAO.updateSaldo(usuario.getIdUsuario(), nuevoSaldo);
        SessionManager.getInstancia().refrescarSaldo(nuevoSaldo);

        Transaccion t = new Transaccion();
        t.setIdUsuario(usuario.getIdUsuario());
        t.setTipo(TipoTransaccion.APUESTA);
        t.setMonto(monto);
        t.setDescripcion("Apuesta carrera #" + carrera.getIdCarrera() + " — Caballo: " + seleccionado.getNombre());
        transaccionDAO.insert(t);

        detenerTimeline();
        cerrarVentana();
    }

    @FXML
    private void handleCancelar() {
        detenerTimeline();
        cerrarVentana();
    }

    private void detenerTimeline() {
        if (cierreTimeline != null) cierreTimeline.stop();
    }

    private void cerrarVentana() {
        ((Stage) confirmBtn.getScene().getWindow()).close();
    }
}
