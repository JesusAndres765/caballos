package Controller;

import Controller.util.CaballoTask;
import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.*;
import Model.dao.*;
import Model.enums.EstadoCarrera;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
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
    private final List<Pane> carrilesHorse = new ArrayList<>();
    private MediaPlayer playerFanfarria;  // referencia para poder detenerla al volver
    private boolean     muerteSubitaActiva = false;

    // ── Visual: caballos e imágenes ──────────────────────────────────────────
    private final List<ImageView> imagenesHorse = new ArrayList<>();
    private final List<Image[]>   spritesHorse  = new ArrayList<>();
    private Image                 imgPista;

    // Ancho disponible en el carril para que el caballo se desplace (px)
    private static final double TRACK_WIDTH = 520.0;

    // ── Timelines ────────────────────────────────────────────────────────────
    private Timeline gateraTimeline;
    private Timeline animTimeline;
    private Timeline spriteTimeline;
    private Timeline pollingTimeline;

    // ── Audio ────────────────────────────────────────────────────────────────
    private MediaPlayer playerGalope;
    private MediaPlayer playerRelincho;

    // ── DAOs ─────────────────────────────────────────────────────────────────
    private final CarreraDAO        carreraDAO        = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO        caballoDAO        = new CaballoDAO();
    private final UsuarioDAO        usuarioDAO        = new UsuarioDAO();

    // ── Inicialización ───────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));

        // Precargar imagen de pista para los carriles
        imgPista = intentarCargar("/images/pista.png");
    }

    public void setCarrera(Carrera carrera) {
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

        // Crea los carriles visuales
        for (int i = 0; i < caballosList.size(); i++) {
            crearFilaCaballo(caballosList.get(i), i);
        }

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

    /**
     * Crea la fila visual de un caballo:
     *  [Nombre | Carril con imagen de caballo animada | Estado]
     *
     * @param caballo el caballo a representar
     * @param index   posición en la lista (0-based), usado para cargar sprites específicos
     */
    private void crearFilaCaballo(Caballo caballo, int index) {
        // Intenta cargar sprites específicos del caballo; si no existen, usa el 0 (genérico)
        Image s1 = intentarCargar("/images/caballos/caballo" + (index + 1) + "-1.png");
        Image s2 = intentarCargar("/images/caballos/caballo" + (index + 1) + "-2.png");
        if (s1 == null) s1 = intentarCargar("/images/caballos/caballo0-1.png");
        if (s2 == null) s2 = intentarCargar("/images/caballos/caballo0-2.png");
        spritesHorse.add(new Image[]{s1, s2});

        ImageView iv = new ImageView(s1 != null ? s1 : null);
        iv.setFitHeight(50);
        iv.setPreserveRatio(true);
        iv.setLayoutX(0);
        iv.setLayoutY(15);

        Pane carril = new Pane(iv);
        carril.setPrefSize(600, 80);
        carril.setMinWidth(300);
        HBox.setHgrow(carril, Priority.ALWAYS);

        if (imgPista != null) {
            carril.setStyle("-fx-background-image: url('" + imgPista.getUrl() + "'); -fx-background-size: 100% 100%;");
        } else {
            carril.setStyle("-fx-background-color: linear-gradient(to bottom, #5a8a3a, #4a7a2a);");
        }

        // ── NUEVO: guarda referencia del carril ──────────────────────────────────
        carrilesHorse.add(carril);
        final Pane carrilRef = carril;
        // ────────────────────────────────────────────────────────────────────────

        Label lblNombre = new Label("No." + caballo.getNumero() + " — " + caballo.getNombre());
        lblNombre.setPrefWidth(130);
        lblNombre.setWrapText(true);
        lblNombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #ECEFF1; -fx-font-size: 13px;");

        Label lblEstado = new Label("En espera");
        lblEstado.setPrefWidth(100);
        lblEstado.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");

        HBox fila = new HBox(8, lblNombre, carril, lblEstado);
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        caballosContainer.getChildren().add(fila);

        imagenesHorse.add(iv);
        etiquetasEstado.add(lblEstado);
    }

    // ── Cuenta regresiva (EN_GATERA) ─────────────────────────────────────────

    private void iniciarCuentaRegresivaGatera() {
        LocalDateTime inicio = carrera.getFechaInicio();
        if (inicio == null) {
            reproducirFanfarriaYLuego(this::retomarCarreraEnCurso);
            return;
        }

        final long[] segundos = { ChronoUnit.SECONDS.between(LocalDateTime.now(), inicio) };

        if (segundos[0] <= 0) {
            reproducirFanfarriaYLuego(this::retomarCarreraEnCurso);
            return;
        }

        timerLabel.setText(formatearTiempo(segundos[0]));

        gateraTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundos[0]--;
            if (segundos[0] > 0) {
                timerLabel.setText(formatearTiempo(segundos[0]));
            } else {
                gateraTimeline.stop();
                tituloLabel.setText("¡Preparados!");
                timerLabel.setText("🎺");

                // La fanfarria suena y al TERMINAR arranca la carrera
                reproducirFanfarriaYLuego(() -> {
                    // Releer estado fresco de BD (CarreraService ya la transitó)
                    Carrera actual = carreraDAO.findById(carrera.getIdCarrera());
                    if (actual != null) carrera = actual;

                    if (carrera.getEstado() == EstadoCarrera.FINALIZADA) {
                        mostrarFinalizacion();
                    } else {
                        tituloLabel.setText("Carrera en Curso");
                        retomarCarreraEnCurso(); // arranca desde la posición real
                    }
                });
            }
        }));
        gateraTimeline.setCycleCount(Timeline.INDEFINITE);
        gateraTimeline.play();
    }

    /**
     * Reproduce la fanfarria y ejecuta [despues] al terminar.
     * Si no existe el archivo, ejecuta [despues] inmediatamente.
     */
    private void reproducirFanfarriaYLuego(Runnable despues) {
        var url = getClass().getResource("/sounds/fanfarria.wav");
        if (url == null) {
            despues.run();
            return;
        }
        try {
            playerFanfarria = new MediaPlayer(new Media(url.toExternalForm()));
            // Cuando termina la fanfarria → arranca la carrera en el hilo FX
            playerFanfarria.setOnEndOfMedia(() -> Platform.runLater(despues));
            playerFanfarria.play();
        } catch (Exception e) {
            System.err.println("Fanfarria: " + e.getMessage());
            despues.run(); // fallback si hay error de audio
        }
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

    // ── Animación visual ──────────────────────────────────────────────────────

    /**
     * Inicia la animación de caballos corriendo.
     * Es puramente cosmética — CarreraService maneja el estado real en BD.
     *
     * @param progresoInicial 0-100: posición de partida (0 = inicio, >0 = retomar mid-race)
     * @param segsRestantes   segundos que muestra el timer
     */
    private void iniciarAnimacionVisual(double progresoInicial, int segsRestantes) {
        // La fanfarria ya fue reproducida antes — aquí solo el ambiente
        iniciarSonidoAmbiente();
        muerteSubitaActiva = false;

        for (int i = 0; i < caballosList.size(); i++) {
            CaballoTask tarea = new CaballoTask(carrera.getDuracionSeg(), progresoInicial);
            final int    idx      = i;
            final ImageView iv    = imagenesHorse.get(i);
            final Label lblEstado = etiquetasEstado.get(i);
            final Caballo cab     = caballosList.get(i);

            final Pane carrilRef = carrilesHorse.get(i);
            Platform.runLater(() -> {
                double finishX = carrilRef.getWidth() - 90;
                iv.setLayoutX(progresoInicial / 100.0 * Math.max(1, finishX));
            });

            tarea.progressProperty().addListener((obs, oldVal, newVal) -> {
                double finishX = carrilRef.getWidth() - 90; // 90 ≈ ancho del sprite + margen antes de la bandera
                iv.setLayoutX(newVal.doubleValue() * Math.max(0, finishX));
                int pct = (int)(newVal.doubleValue() * 100);
                if (pct < 100) lblEstado.setText(pct + "%");
            });

            tarea.setOnSucceeded(ev -> {
                double finishX = carrilRef.getWidth() - 90;
                iv.setLayoutX(Math.max(0, finishX)); // caballo llega justo a la meta
                lblEstado.setText("Terminó");
                if (muerteSubitaActiva) {
                    muerteSubitaActiva = false;
                    tituloLabel.setText("⚡ ¡" + cab.getNombre() + " gana la muerte súbita!");
                    if (spriteTimeline != null) spriteTimeline.stop();
                    if (playerGalope   != null) playerGalope.stop();
                }
            });

            tareas.add(tarea);
            Thread hilo = new Thread(tarea);
            hilo.setDaemon(true);
            hilo.start();
        }

        // Sprites alternando cada 120 ms
        final int[] frame = {0};
        spriteTimeline = new Timeline(new KeyFrame(Duration.millis(120), e -> {
            frame[0]++;
            for (int i = 0; i < imagenesHorse.size(); i++) {
                Image[] sprites = spritesHorse.get(i);
                if (sprites[0] != null)
                    imagenesHorse.get(i).setImage(frame[0] % 2 == 0 ? sprites[0] : sprites[1]);
            }
        }));
        spriteTimeline.setCycleCount(Timeline.INDEFINITE);
        spriteTimeline.play();

        // Timer visual — al llegar a 0 verifica si hay muerte súbita
        final int[] secs = { segsRestantes };
        timerLabel.setText(formatearTiempo(secs[0]));
        animTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secs[0]--;
            timerLabel.setText(secs[0] > 0 ? formatearTiempo(secs[0]) : "0:00");
        }));
        animTimeline.setCycleCount(segsRestantes);
        animTimeline.setOnFinished(e -> verificarMuerteSubitaAlTerminar());
        animTimeline.play();
    }

    /**
     * Llamado cuando el timer visual llega a 0.
     * Si ningún caballo terminó, activa la muerte súbita:
     * las tareas siguen corriendo y el primero en llegar a 100% gana.
     */
    private void verificarMuerteSubitaAlTerminar() {
        boolean algTermino = tareas.stream()
                .anyMatch(t -> t.getProgresoActual() >= 100.0);

        if (!algTermino && !tareas.isEmpty()) {
            muerteSubitaActiva = true;
            tituloLabel.setText("⚡ ¡MUERTE SÚBITA! ⚡");
            timerLabel.setText("¡AL PRIMERO!");
            // Las CaballoTasks siguen corriendo — su setOnSucceeded resuelve el ganador
        }
        // Si alguno terminó, el polling detectará FINALIZADA y cerrará normalmente
    }

    // ── Polling — detecta cuando CarreraService finaliza la carrera ───────────

    private void iniciarPolling() {
        if (pollingTimeline != null) pollingTimeline.stop();
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
        limpiar(); // detiene animación, sprites y audio
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
                            : "Lugar #" + cc.getPosicionFinal();
                    etiquetasEstado.get(i).setText(texto);
                    // Mueve el caballo al final del carril si terminó en 1er lugar
                    if (cc.getPosicionFinal() == 1 && i < carrilesHorse.size()) {
                        double finishX = carrilesHorse.get(i).getWidth() - 90;
                        imagenesHorse.get(i).setLayoutX(Math.max(0, finishX));
                    }
                    break;
                }
            }
        }
    }

    // ── Audio ─────────────────────────────────────────────────────────────────

    private void reproducirFanfarria() {
        try {
            var url = getClass().getResource("/sounds/fanfarria.wav");
            if (url == null) return;
            new javafx.scene.media.AudioClip(url.toExternalForm()).play();
        } catch (Exception e) {
            System.err.println("Audio fanfarria: " + e.getMessage());
        }
    }

    private void iniciarSonidoAmbiente() {
        playerRelincho = reproducirMedia("/sounds/relincho.wav", false); // una vez
        playerGalope   = reproducirMedia("/sounds/correr.wav",   true);  // en bucle
    }

    private MediaPlayer reproducirMedia(String ruta, boolean bucle) {
        var url = getClass().getResource(ruta);
        if (url == null) return null;
        try {
            MediaPlayer mp = new MediaPlayer(new Media(url.toExternalForm()));
            if (bucle) mp.setCycleCount(MediaPlayer.INDEFINITE);
            mp.play();
            return mp;
        } catch (Exception e) {
            System.err.println("Audio " + ruta + ": " + e.getMessage());
            return null;
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private Image intentarCargar(String ruta) {
        var url = getClass().getResource(ruta);
        return url != null ? new Image(url.toExternalForm()) : null;
    }

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
        if (spriteTimeline  != null) spriteTimeline.stop();
        if (pollingTimeline != null) pollingTimeline.stop();
        if (playerFanfarria  != null) playerFanfarria.stop();
        if (playerGalope    != null) playerGalope.stop();
        if (playerRelincho  != null) playerRelincho.stop();
        tareas.forEach(t -> t.cancel(false));
    }
}