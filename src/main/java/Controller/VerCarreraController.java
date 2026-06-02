package Controller;

import Controller.util.CaballoTask;
import Controller.util.SessionManager;
import Model.*;
import Model.dao.*;
import Model.enums.EstadoCarrera;
import Model.enums.ResultadoApuesta;
import Model.enums.TipoTransaccion;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class VerCarreraController {

    @FXML private Label  usuarioLabel;
    @FXML private Label  saldoLabel;
    @FXML private Label  tituloLabel;
    @FXML private Label  timerLabel;
    @FXML private VBox   caballosContainer;
    @FXML private Button verResultadosBtn;

    private Carrera             carrera;
    private List<CarreraCaballo> inscripciones = new ArrayList<>();
    private List<Caballo>        caballosList  = new ArrayList<>();
    private List<CaballoTask>    tareas        = new ArrayList<>();
    private List<Label>          etiquetasEstado = new ArrayList<>();
    private List<ProgressBar>    progressBars    = new ArrayList<>();

    private Timeline gateraTimeline;
    private Timeline carreraTimeline;

    private final CarreraDAO         carreraDAO        = new CarreraDAO();
    private final CarreraCaballoDAO  carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO         caballoDAO        = new CaballoDAO();
    private final ApuestaDAO         apuestaDAO        = new ApuestaDAO();
    private final UsuarioDAO         usuarioDAO        = new UsuarioDAO();
    private final TransaccionDAO     transaccionDAO    = new TransaccionDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));

        // Registra handler de cierre una vez que la ventana exista
        Platform.runLater(() -> {
            Stage stage = (Stage) caballosContainer.getScene().getWindow();
            if (stage != null) stage.setOnCloseRequest(e -> limpiar());
        });
    }

    // Punto de entrada — llamado desde DashboardUsuarioController
    public void setCarrera(Carrera carrera) {
        this.carrera = carrera;
        cargarDatos();
    }

    private void cargarDatos() {
        inscripciones = carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
        for (CarreraCaballo cc : inscripciones) {
            Caballo c = caballoDAO.findById(cc.getIdCaballo());
            if (c != null) caballosList.add(c);
        }
        for (int i = 0; i < caballosList.size(); i++) {
            crearFilaCaballo(caballosList.get(i));
        }

        switch (carrera.getEstado()) {
            case EN_GATERA:
                tituloLabel.setText("Carrera por Empezar");
                iniciarCuentaRegresivaGatera();
                break;
            case EN_CURSO:
                tituloLabel.setText("Carrera en Curso");
                iniciarCarrera();
                break;
            case FINALIZADA:
                tituloLabel.setText("Carrera Finalizada");
                timerLabel.setText("0:00");
                verResultadosBtn.setDisable(false);
                break;
            default:
                tituloLabel.setText("Carrera #" + carrera.getIdCarrera());
        }
    }

    private void crearFilaCaballo(Caballo caballo) {
        Label nombreLabel = new Label(caballo.getNombre() + " | No. " + caballo.getNumero());
        nombreLabel.setPrefWidth(170);

        ProgressBar pb = new ProgressBar(0);
        pb.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pb, Priority.ALWAYS);

        Label estadoLabel = new Label("En espera");
        estadoLabel.setPrefWidth(120);

        HBox fila = new HBox(10, nombreLabel, pb, estadoLabel);
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        caballosContainer.getChildren().add(fila);
        progressBars.add(pb);
        etiquetasEstado.add(estadoLabel);
    }

    private void iniciarCuentaRegresivaGatera() {
        LocalDateTime inicio  = carrera.getFechaCreacion().plusMinutes(carrera.getTiempoGatera());
        final long[] segundos = { ChronoUnit.SECONDS.between(LocalDateTime.now(), inicio) };

        if (segundos[0] <= 0) {
            iniciarCarrera();
            return;
        }

        timerLabel.setText(formatearTiempo(segundos[0]));

        gateraTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundos[0]--;
            timerLabel.setText(formatearTiempo(segundos[0]));
            if (segundos[0] <= 0) {
                gateraTimeline.stop();
                iniciarCarrera();
            }
        }));
        gateraTimeline.setCycleCount(Timeline.INDEFINITE);
        gateraTimeline.play();
    }

    private void iniciarCarrera() {
        tituloLabel.setText("Carrera en Curso");
        carreraDAO.updateEstado(carrera.getIdCarrera(), EstadoCarrera.EN_CURSO);
        carreraDAO.updateFechaInicio(carrera.getIdCarrera(), LocalDateTime.now());

        for (int i = 0; i < caballosList.size(); i++) {
            CaballoTask tarea = new CaballoTask(carrera.getDuracionSeg());
            final int idx = i;

            // Enlaza la barra de progreso al Task — actualización automática y thread-safe
            progressBars.get(idx).progressProperty().bind(tarea.progressProperty());

            // Cuando un caballo alcanza 100%, actualiza su etiqueta en el hilo de JavaFX
            tarea.setOnSucceeded(ev -> etiquetasEstado.get(idx).setText("Terminó"));

            tareas.add(tarea);

            Thread hilo = new Thread(tarea);
            hilo.setDaemon(true);
            hilo.start();
        }

        // Cronómetro de duración de la carrera
        final int[] segsCarrera = { carrera.getDuracionSeg() };
        timerLabel.setText(formatearTiempo(segsCarrera[0]));

        carreraTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segsCarrera[0]--;
            timerLabel.setText(formatearTiempo(segsCarrera[0]));
            if (segsCarrera[0] <= 0) {
                carreraTimeline.stop();
                finalizarCarrera();
            }
        }));
        carreraTimeline.setCycleCount(Timeline.INDEFINITE);
        carreraTimeline.play();
    }

    private void finalizarCarrera() {
        for (CaballoTask t : tareas) t.cancel(false);

        // PauseTransition no bloquea el hilo de JavaFX: espera 200ms y luego procesa
        PauseTransition pausa = new PauseTransition(Duration.millis(200));
        pausa.setOnFinished(e -> procesarResultados());
        pausa.play();
    }

    private void procesarResultados() {
        // Lista {índice, progreso*100} para ordenar sin perder referencia al índice original
        List<int[]> ordenados = new ArrayList<>();
        for (int i = 0; i < tareas.size(); i++) {
            ordenados.add(new int[]{ i, (int)(tareas.get(i).getProgresoActual() * 100) });
        }
        ordenados.sort((a, b) -> b[1] - a[1]); // mayor progreso primero

        int idCaballoGanador = -1;

        for (int pos = 0; pos < ordenados.size(); pos++) {
            int          idx     = ordenados.get(pos)[0];
            CaballoTask  tarea   = tareas.get(idx);
            CarreraCaballo cc    = inscripciones.get(idx);
            Caballo      caballo = caballosList.get(idx);

            double  progreso = tarea.getProgresoActual();
            boolean termino  = tarea.terminoCarrera();
            int     posicion = pos + 1;

            carreraCaballoDAO.updateResultado(cc.getId(), posicion, progreso, termino);
            caballoDAO.actualizarContadores(caballo.getIdCaballo(), pos == 0);

            String textoEstado = termino
                    ? "Lugar #" + posicion
                    : "Lugar #" + posicion + " (no terminó)";
            etiquetasEstado.get(idx).setText(textoEstado);

            if (pos == 0) idCaballoGanador = caballo.getIdCaballo();
        }

        carreraDAO.updateEstado(carrera.getIdCarrera(), EstadoCarrera.FINALIZADA);
        liquidarApuestas(idCaballoGanador);

        // Refresca saldo por si el usuario ganó alguna apuesta
        Usuario refreshed = usuarioDAO.findById(
                SessionManager.getInstance().getUsuarioActual().getIdUsuario()
        );
        if (refreshed != null) {
            SessionManager.getInstance().refrescarSaldo(refreshed.getSaldo());
            saldoLabel.setText(String.format("Saldo: %.2f", refreshed.getSaldo()));
        }

        verResultadosBtn.setDisable(false);
        tituloLabel.setText("Carrera Finalizada");
        timerLabel.setText("0:00");
    }

    private void liquidarApuestas(int idCaballoGanador) {
        List<Apuesta> apuestas = apuestaDAO.findByCarrera(carrera.getIdCarrera());

        for (Apuesta a : apuestas) {
            if (a.getIdCaballo() == idCaballoGanador) {
                double cobro = a.getMonto() * a.getMultiplicador();
                apuestaDAO.liquidar(a.getIdApuesta(), ResultadoApuesta.GANADA, cobro);

                Usuario ganador = usuarioDAO.findById(a.getIdUsuario());
                if (ganador != null) {
                    double nuevoSaldo = ganador.getSaldo() + cobro;
                    usuarioDAO.updateSaldo(ganador.getIdUsuario(), nuevoSaldo);

                    Transaccion t = new Transaccion();
                    t.setIdUsuario(ganador.getIdUsuario());
                    t.setTipo(TipoTransaccion.COBRO);
                    t.setMonto(cobro);
                    t.setDescripcion("Cobro apuesta carrera #" + carrera.getIdCarrera());
                    transaccionDAO.insert(t);
                }
            } else {
                apuestaDAO.liquidar(a.getIdApuesta(), ResultadoApuesta.PERDIDA, 0.0);
            }
        }
    }

    private String formatearTiempo(long seg) {
        return String.format("%d:%02d", seg / 60, Math.abs(seg % 60));
    }

    @FXML
    private void handleVerResultados() {
        // Se conecta con ResultadosController en el siguiente bloque
    }

    @FXML
    private void handleVolver() {
        limpiar();
        ((Stage) caballosContainer.getScene().getWindow()).close();
    }

    private void limpiar() {
        if (gateraTimeline  != null) gateraTimeline.stop();
        if (carreraTimeline != null) carreraTimeline.stop();
        tareas.forEach(t -> t.cancel(false));
    }

    // Usado por el Stage's close handler configurado en initialize()
    public VBox getCaballosContainer() { return caballosContainer; }
}