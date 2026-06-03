package Controller;

import Controller.util.CaballoTask;
import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.*;
import Model.dao.*;
import Model.enums.EstadoCarrera;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
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

    private Carrera              carrera;
    private List<CarreraCaballo> inscripciones   = new ArrayList<>();
    private List<Caballo>        caballosList    = new ArrayList<>();
    private List<CaballoTask>    tareas          = new ArrayList<>();
    private List<Label>          etiquetasEstado = new ArrayList<>();
    private List<ProgressBar>    progressBars    = new ArrayList<>();

    private Timeline gateraTimeline;  // cuenta regresiva de espera
    private Timeline animTimeline;    // countdown visual de la carrera (cosmético)
    private Timeline pollingTimeline; // detecta cambios de estado en BD

    private final CarreraDAO        carreraDAO        = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO        caballoDAO        = new CaballoDAO();
    private final UsuarioDAO        usuarioDAO        = new UsuarioDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
    }

    public void setCarrera(Carrera carrera) {
        // Siempre lee el estado más reciente de BD por si el servicio
        // ya transitó la carrera mientras el usuario estaba en otra pantalla
        Carrera fresca = carreraDAO.findById(carrera.getIdCarrera());
        this.carrera = (fresca != null) ? fresca : carrera;
        cargarDatos();
    }

    // ── Carga y despacho según estado ────────────────────────────────────────

    private void cargarDatos() {
        inscripciones = carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
        for (CarreraCaballo cc : inscripciones) {
            Caballo c = caballoDAO.findById(cc.getIdCaballo());
            if (c != null) caballosList.add(c);
        }
        for (Caballo c : caballosList) crearFilaCaballo(c);

        switch (carrera.getEstado()) {
            case EN_GATERA:
                tituloLabel.setText("Carrera por Empezar");
                iniciarCuentaRegresivaGatera();
                break;
            case EN_CURSO:
                tituloLabel.setText("Carrera en Curso");
                retomarCarreraEnCurso();
                break;
            case FINALIZADA:
                tituloLabel.setText("Carrera Finalizada");
                timerLabel.setText("0:00");
                mostrarResultadosEnUI();
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

    // ── Cuenta regresiva (EN_GATERA) ─────────────────────────────────────────

    private void iniciarCuentaRegresivaGatera() {
        LocalDateTime inicio = carrera.getFechaInicio();
        if (inicio == null) {
            iniciarAnimacionVisual(0.0, carrera.getDuracionSeg());
            iniciarPolling();
            return;
        }

        final long[] segundos = { ChronoUnit.SECONDS.between(LocalDateTime.now(), inicio) };

        if (segundos[0] <= 0) {
            iniciarAnimacionVisual(0.0, carrera.getDuracionSeg());
            iniciarPolling();
            return;
        }

        timerLabel.setText(formatearTiempo(segundos[0]));

        gateraTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundos[0]--;
            if (segundos[0] > 0) {
                timerLabel.setText(formatearTiempo(segundos[0]));
            } else {
                gateraTimeline.stop();
                tituloLabel.setText("Carrera en Curso");
                iniciarAnimacionVisual(0.0, carrera.getDuracionSeg());
                iniciarPolling();
            }
        }));
        gateraTimeline.setCycleCount(Timeline.INDEFINITE);
        gateraTimeline.play();
    }

    // ── Retomar carrera ya iniciada (EN_CURSO) ────────────────────────────────

    private void retomarCarreraEnCurso() {
        LocalDateTime fechaInicio = carrera.getFechaInicio();
        if (fechaInicio == null) {
            iniciarAnimacionVisual(0.0, carrera.getDuracionSeg());
            iniciarPolling();
            return;
        }

        long elapsedMs   = ChronoUnit.MILLIS.between(fechaInicio, LocalDateTime.now());
        long totalMs     = carrera.getDuracionSeg() * 1000L;
        long remainingMs = totalMs - elapsedMs;

        if (remainingMs <= 0) {
            timerLabel.setText("0:00");
            iniciarPolling();
            return;
        }

        double progresoEstimado = (double) elapsedMs / totalMs * 85.0;
        iniciarAnimacionVisual(progresoEstimado, (int)(remainingMs / 1000));
        iniciarPolling();
    }

    // ── Animación visual (puramente cosmética — CarreraService maneja la BD) ──

    private void iniciarAnimacionVisual(double progresoInicial, int segsRestantes) {
        // Solo reproduce la fanfarria cuando la carrera arranca desde el inicio
        if (progresoInicial == 0) {
            reproducirFanfarria();
        }

        for (int i = 0; i < caballosList.size(); i++) {
            CaballoTask tarea = new CaballoTask(carrera.getDuracionSeg(), progresoInicial);
            final int idx = i;
            progressBars.get(idx).progressProperty().bind(tarea.progressProperty());
            tarea.setOnSucceeded(ev -> etiquetasEstado.get(idx).setText("Terminó"));
            tareas.add(tarea);
            Thread hilo = new Thread(tarea);
            hilo.setDaemon(true);
            hilo.start();
        }

        final int[] secs = { segsRestantes };
        timerLabel.setText(formatearTiempo(secs[0]));

        // setCycleCount hace que el timeline se detenga solo — no llama a finalizarCarrera()
        // porque el CarreraService es quien maneja eso en segundo plano
        animTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secs[0]--;
            timerLabel.setText(secs[0] > 0 ? formatearTiempo(secs[0]) : "0:00");
        }));
        animTimeline.setCycleCount(segsRestantes);
        animTimeline.play();
    }

    // ── Polling — detecta cuando CarreraService finaliza la carrera ───────────

    private void iniciarPolling() {
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            Carrera actual = carreraDAO.findById(carrera.getIdCarrera());
            if (actual == null) return;
            if (actual.getEstado() == EstadoCarrera.FINALIZADA
                    && carrera.getEstado() != EstadoCarrera.FINALIZADA) {
                pollingTimeline.stop();
                carrera = actual;
                mostrarFinalizacion();
            }
        }));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    // ── Pantalla de finalización ──────────────────────────────────────────────

    private void mostrarFinalizacion() {
        limpiar();
        mostrarResultadosEnUI();
        tituloLabel.setText("Carrera Finalizada");
        timerLabel.setText("0:00");
        verResultadosBtn.setDisable(false);

        // Refresca saldo por si CarreraService acreditó una apuesta ganada
        Usuario refreshed = usuarioDAO.findById(
                SessionManager.getInstance().getUsuarioActual().getIdUsuario());
        if (refreshed != null) {
            SessionManager.getInstance().refrescarSaldo(refreshed.getSaldo());
            saldoLabel.setText(String.format("Saldo: %.2f", refreshed.getSaldo()));
        }
    }

    private void mostrarResultadosEnUI() {
        List<CarreraCaballo> resultados =
                carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
        for (CarreraCaballo cc : resultados) {
            if (cc.getPosicionFinal() == 0) continue;
            for (int i = 0; i < inscripciones.size(); i++) {
                if (inscripciones.get(i).getIdCaballo() == cc.getIdCaballo()) {
                    String texto = cc.isTerminoCarrera()
                            ? "Lugar #" + cc.getPosicionFinal()
                            : "Lugar #" + cc.getPosicionFinal() + " (no terminó)";
                    etiquetasEstado.get(i).setText(texto);
                    break;
                }
            }
        }
    }

    // ── Tu fanfarria original — se conserva igual ─────────────────────────────

    private void reproducirFanfarria() {
        try {
            String rutaAudio = getClass().getResource("/sounds/fanfarria.wav").toExternalForm();
            AudioClip clip = new AudioClip(rutaAudio);
            clip.play();
        } catch (Exception e) {
            System.err.println("No se pudo reproducir el sonido: " + e.getMessage());
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private String formatearTiempo(long seg) {
        long s = Math.max(0, seg);
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @FXML
    private void handleVerResultados() {
        limpiar();
        FXMLLoader loader = SceneManager.cambiarEscena("resultados.fxml");
        if (loader != null) {
            ResultadosController ctrl = loader.getController();
            ctrl.setCarrera(carrera);
        }
    }

    @FXML
    private void handleVolver() {
        limpiar();
        SceneManager.cambiarEscena("dashboard-usuario.fxml");
    }

    private void limpiar() {
        if (gateraTimeline  != null) gateraTimeline.stop();
        if (animTimeline    != null) animTimeline.stop();
        if (pollingTimeline != null) pollingTimeline.stop();
        tareas.forEach(t -> t.cancel(false));
    }
}