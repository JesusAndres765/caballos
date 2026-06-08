package Controller;

import Controller.util.CaballoTask;
import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Caballo;
import Model.Carrera;
import Model.CarreraCaballo;
import Model.Usuario;
import Model.dao.CarreraDAO;
import Model.dao.CarreraCaballoDAO;
import Model.dao.CaballoDAO;
import Model.dao.UsuarioDAO;
import Model.enums.EstadoCarrera;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class VerCarreraController {
    @FXML private Label usuarioLabel;
    @FXML private Label saldoLabel;
    @FXML private Label tituloLabel;
    @FXML private Label timerLabel;
    @FXML private VBox caballosContainer;
    @FXML private Button verResultadosBtn;

    private Carrera carrera;
    private List<CarreraCaballo> inscripciones = new ArrayList<>();
    private List<Caballo> caballosList = new ArrayList<>();
    private List<CaballoTask> tareas = new ArrayList<>();
    private List<Label> etiquetasEstado = new ArrayList<>();
    private List<Pane> carrilesCaballos = new ArrayList<>();
    private List<ImageView> imagenesHorse = new ArrayList<>();
    private List<Image[]> spritesHorse = new ArrayList<>();

    private boolean muerteSubitaActiva = false;

    private Timeline gateraTimeline;
    private Timeline animTimeline;
    private Timeline spriteTimeline;
    private Timeline pollingTimeline;

    private MediaPlayer playerGalope;
    private MediaPlayer playerRelincho;

    private final CarreraDAO carreraDAO = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO caballoDAO = new CaballoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstancia().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
    }

    public void setCarrera(Carrera carrera) {
        Carrera fresca = carreraDAO.buscarId(carrera.getIdCarrera());
        this.carrera = (fresca != null) ? fresca : carrera;
        cargarDatos();
    }

    // Carga los caballos y muestra segun el estado actual de la carrera
    private void cargarDatos() {
        inscripciones = carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
        for (CarreraCaballo cc : inscripciones) {
            Caballo c = caballoDAO.findById(cc.getIdCaballo());
            if (c != null) caballosList.add(c);
        }
        for (int i = 0; i < caballosList.size(); i++) {
            crearFilaCaballo(caballosList.get(i), i);
        }

        if (carrera.getEstado() == EstadoCarrera.EN_GATERA) {
            tituloLabel.setText("Carrera por Empezar");
            iniciarCuentaRegresivaGatera();

        } else if (carrera.getEstado() == EstadoCarrera.EN_CURSO) {
            tituloLabel.setText("Carrera en Curso");
            retomarCarreraEnCurso();

        } else if (carrera.getEstado() == EstadoCarrera.FINALIZADA) {
            tituloLabel.setText("Carrera Finalizada");
            timerLabel.setText("0:00");
            mostrarResultadosEnUI();
            verResultadosBtn.setDisable(false);

        } else {
            tituloLabel.setText("Carrera #" + carrera.getIdCarrera());
        }
    }

    // Crea la fila de un caballo [Nombre | Carril | Estado]
    private void crearFilaCaballo(Caballo caballo, int index) {
        Image sprite1 = cargarImagen("/images/caballos/caballo" + (index + 1) + "-1.png");
        Image sprite2 = cargarImagen("/images/caballos/caballo" + (index + 1) + "-2.png");
        spritesHorse.add(new Image[]{ sprite1, sprite2 });

        ImageView imageView = new ImageView(sprite1 != null ? sprite1 : null);
        imageView.setFitHeight(50);
        imageView.setPreserveRatio(true);
        imageView.setLayoutX(0);
        imageView.setLayoutY(15);

        Pane carril = new Pane(imageView);
        carril.setPrefSize(600, 80);
        carril.setMinWidth(300);
        HBox.setHgrow(carril, Priority.ALWAYS);

        Image imgPista = cargarImagen("/images/pista.png");
        if (imgPista != null) {
            carril.setStyle("-fx-background-image: url('" + imgPista.getUrl() + "'); -fx-background-size: 100% 100%;");
        } else {
            carril.setStyle("-fx-background-color: linear-gradient(to bottom, #5a8a3a, #4a7a2a);");
        }

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

        imagenesHorse.add(imageView);
        carrilesCaballos.add(carril);
        etiquetasEstado.add(lblEstado);
    }

    // Cuenta regresiva mientras la carrera esta esperando iniciar
    private void iniciarCuentaRegresivaGatera() {
        LocalDateTime inicio = carrera.getFechaInicio();

        if (inicio == null) {
            reproducirFanfarria();
            retomarCarreraEnCurso();
            return;
        }

        long[] segundos = { ChronoUnit.SECONDS.between(LocalDateTime.now(), inicio) };

        if (segundos[0] <= 0) {
            reproducirFanfarria();
            retomarCarreraEnCurso();
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
                reproducirFanfarria();
                retomarCarreraEnCurso();
            }
        }));
        gateraTimeline.setCycleCount(Timeline.INDEFINITE);
        gateraTimeline.play();
    }

    // Retoma la animacion de una carrera que ya estaba en curso
    private void retomarCarreraEnCurso() {
        LocalDateTime fechaInicio = carrera.getFechaInicio();

        if (fechaInicio == null) {
            iniciarAnimacionVisual(0.0, carrera.getDuracionSeg());
            iniciarPolling();
            return;
        }

        long elapsedMs = ChronoUnit.MILLIS.between(fechaInicio, LocalDateTime.now());
        long totalMs  = carrera.getDuracionSeg() * 1000L;
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

    private void iniciarAnimacionVisual(double progresoInicial, int segsRestantes) {
        iniciarSonidoAmbiente();
        muerteSubitaActiva = false;

        for (int i = 0; i < caballosList.size(); i++) {
            CaballoTask tarea = new CaballoTask(carrera.getDuracionSeg(), progresoInicial);
            final int idx = i;
            final ImageView iv = imagenesHorse.get(i);
            final Label lblEstado = etiquetasEstado.get(i);
            final Caballo cab = caballosList.get(i);
            final Pane carril = carrilesCaballos.get(i);

            Platform.runLater(() -> {
                double metaX = carril.getWidth() - 90;
                iv.setLayoutX(progresoInicial / 100.0 * Math.max(1, metaX));
            });

            // Mueve el sprite y actualiza el porcentaje cada vez que avanza
            tarea.progressProperty().addListener((obs, oldVal, newVal) -> {
                double metaX = carril.getWidth() - 90;
                iv.setLayoutX(newVal.doubleValue() * Math.max(0, metaX));
                int pct = (int)(newVal.doubleValue() * 100);
                if (pct < 100) lblEstado.setText(pct + "%");
            });

            // Cuando el caballo llega a la meta
            tarea.setOnSucceeded(ev -> {
                double metaX = carril.getWidth() - 90;
                iv.setLayoutX(Math.max(0, metaX));
                lblEstado.setText("Terminó");

                if (muerteSubitaActiva) {
                    muerteSubitaActiva = false;
                    tituloLabel.setText(cab.getNombre() + " GANA");
                    if (spriteTimeline != null) spriteTimeline.stop();
                    if (playerGalope   != null) playerGalope.stop();
                }
            });

            tareas.add(tarea);
            Thread hilo = new Thread(tarea);
            hilo.setDaemon(true);
            hilo.start();
        }

        // Alterna los dos frames del sprite cada 120 ms para simular movimiento
        int[] frame = { 0 };
        spriteTimeline = new Timeline(new KeyFrame(Duration.millis(120), e -> {
            frame[0]++;
            for (int i = 0; i < imagenesHorse.size(); i++) {
                Image[] sprites = spritesHorse.get(i);
                if (sprites[0] != null) {
                    imagenesHorse.get(i).setImage(frame[0] % 2 == 0 ? sprites[0] : sprites[1]);
                }
            }
        }));
        spriteTimeline.setCycleCount(Timeline.INDEFINITE);
        spriteTimeline.play();

        int[] secs = { segsRestantes };
        timerLabel.setText(formatearTiempo(secs[0]));
        animTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secs[0]--;
            timerLabel.setText(secs[0] > 0 ? formatearTiempo(secs[0]) : "0:00");
        }));
        animTimeline.setCycleCount(segsRestantes);
        animTimeline.setOnFinished(e -> verificarMuerteSubita());
        animTimeline.play();
    }

    private void verificarMuerteSubita() {
        boolean algTermino = false;
        for (CaballoTask t : tareas) {
            if (t.getProgresoActual() >= 100.0) {
                algTermino = true;
                break;
            }
        }

        if (!algTermino && !tareas.isEmpty()) {
            muerteSubitaActiva = true;
            tituloLabel.setText("MUERTE SUBITA");
            timerLabel.setText("AL PRIMERO");
        }
    }

    private void iniciarPolling() {
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            Carrera actual = carreraDAO.buscarId(carrera.getIdCarrera());
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

    // Muestra la pantalla de carrera terminada y refresca el saldo del usuario
    private void mostrarFinalizacion() {
        limpiar();
        mostrarResultadosEnUI();
        tituloLabel.setText("Carrera Finalizada");
        timerLabel.setText("0:00");
        verResultadosBtn.setDisable(false);

        Usuario refreshed = usuarioDAO.findById(
                SessionManager.getInstancia().getUsuarioActual().getIdUsuario());
        if (refreshed != null) {
            SessionManager.getInstancia().refrescarSaldo(refreshed.getSaldo());
            saldoLabel.setText(String.format("Saldo: %.2f", refreshed.getSaldo()));
        }
    }

    private void mostrarResultadosEnUI() {
        List<CarreraCaballo> resultados = carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
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

    private void reproducirFanfarria() {
        URL url = getClass().getResource("/sounds/fanfarria.wav");
        if (url == null) return;
        try {
            new javafx.scene.media.AudioClip(url.toExternalForm()).play();
        } catch (Exception e) {
            System.err.println("Audio fanfarria: " + e.getMessage());
        }
    }

    private void iniciarSonidoAmbiente() {
        playerRelincho = reproducirAudio("/sounds/relincho.wav", false);
        playerGalope   = reproducirAudio("/sounds/correr.wav",   true);
    }

    private MediaPlayer reproducirAudio(String ruta, boolean enBucle) {
        URL url = getClass().getResource(ruta);
        if (url == null) return null;
        try {
            MediaPlayer mp = new MediaPlayer(new Media(url.toExternalForm()));
            if (enBucle) mp.setCycleCount(MediaPlayer.INDEFINITE);
            mp.play();
            return mp;
        } catch (Exception e) {
            System.err.println("Audio " + ruta + ": " + e.getMessage());
            return null;
        }
    }

    private Image cargarImagen(String ruta) {
        URL url = getClass().getResource(ruta);
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
        if (gateraTimeline != null) gateraTimeline.stop();
        if (animTimeline != null) animTimeline.stop();
        if (spriteTimeline != null) spriteTimeline.stop();
        if (pollingTimeline != null) pollingTimeline.stop();
        if (playerGalope != null) playerGalope.stop();
        if (playerRelincho != null) playerRelincho.stop();
        for (CaballoTask t : tareas) {
            t.cancel(false);
        }
    }
}
