package Controller;

import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Caballo;
import Model.Carrera;
import Model.CarreraCaballo;
import Model.Usuario;
import Model.dao.CaballoDAO;
import Model.dao.CarreraDAO;
import Model.dao.CarreraCaballoDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DashboardUsuarioController {

    @FXML private Label usuarioLabel;
    @FXML private Label saldoLabel;
    @FXML private VBox  carrerasContainer;

    private final CarreraDAO        carreraDAO        = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO        caballoDAO        = new CaballoDAO();

    private final List<Timeline> timelinesActivos = new ArrayList<>();

    @FXML
    private void initialize() {
        actualizarCabecera();
        cargarCarreras();
    }

    // Refresca username y saldo desde SessionManager
    private void actualizarCabecera() {
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
    }

    private void cargarCarreras() {
        timelinesActivos.forEach(Timeline::stop);
        timelinesActivos.clear();
        carrerasContainer.getChildren().clear();

        List<Carrera> carreras = carreraDAO.findActivas();

        if (carreras.isEmpty()) {
            carrerasContainer.getChildren().add(
                    new Label("No hay carreras activas en este momento.")
            );
            return;
        }

        for (Carrera c : carreras) {
            List<Caballo> caballos = getCaballosDeCarrera(c);
            carrerasContainer.getChildren().add(crearTarjetaCarrera(c, caballos));
        }
    }

    private List<Caballo> getCaballosDeCarrera(Carrera carrera) {
        List<CarreraCaballo> inscripciones = carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
        List<Caballo> lista = new ArrayList<>();
        for (CarreraCaballo cc : inscripciones) {
            Caballo c = caballoDAO.findById(cc.getIdCaballo());
            if (c != null) lista.add(c);
        }
        return lista;
    }

    // Construye una tarjeta de carrera completa con tabla de caballos y countdown
    private VBox crearTarjetaCarrera(Carrera carrera, List<Caballo> caballos) {
        VBox tarjeta = new VBox(8);
        tarjeta.setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-padding: 10;");

        // Multiplicador y porcentaje
        double porcentaje = carrera.getNumCaballos() > 0 ? 100.0 / carrera.getNumCaballos() : 0;
        Label multLabel = new Label("Multiplicador: x" + carrera.getNumCaballos());
        Label pctLabel  = new Label(String.format("%%: %.2f", porcentaje));
        HBox statsRow   = new HBox(20);
        statsRow.getChildren().addAll(multLabel, pctLabel);

        // Tabla de caballos
        TableView<Caballo> tabla = crearTablaCaballos(caballos);

        // Fila inferior: botones + countdown
        Label countdownLabel = new Label();
        configurarCuentaRegresiva(carrera, countdownLabel);

        Button apostarBtn    = new Button("Apostar");
        Button verCarreraBtn = new Button("Ver Carrera");
        HBox botonesRow = new HBox(10);
        botonesRow.getChildren().addAll(apostarBtn, verCarreraBtn, countdownLabel);

        apostarBtn.setOnAction(e -> abrirModalApostar(carrera, caballos));
        verCarreraBtn.setOnAction(e -> abrirVerCarrera(carrera));

        tarjeta.getChildren().addAll(statsRow, tabla, botonesRow);
        return tarjeta;
    }

    private TableView<Caballo> crearTablaCaballos(List<Caballo> caballos) {
        TableView<Caballo> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(150);

        TableColumn<Caballo, String> nombreCol = new TableColumn<>("Nombre");
        nombreCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Caballo, Number> numeroCol = new TableColumn<>("Numero");
        numeroCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getNumero()));

        TableColumn<Caballo, Number> corridasCol = new TableColumn<>("Corridas");
        corridasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasCorridas()));

        TableColumn<Caballo, Number> ganadasCol = new TableColumn<>("Ganadas");
        ganadasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasGanadas()));

        tabla.getColumns().addAll(nombreCol, numeroCol, corridasCol, ganadasCol);
        tabla.setItems(FXCollections.observableArrayList(caballos));
        return tabla;
    }

    private void configurarCuentaRegresiva(Carrera carrera, Label label) {
        LocalDateTime inicio  = carrera.getFechaCreacion().plusMinutes(carrera.getTiempoGatera());
        final long[] segundos = { ChronoUnit.SECONDS.between(LocalDateTime.now(), inicio) };

        if (segundos[0] <= 0) {
            label.setText("Iniciando pronto...");
            return;
        }

        label.setText(formatearTiempo(segundos[0]));

        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundos[0]--;
            if (segundos[0] <= 0) {
                label.setText("¡Iniciando!");
            } else {
                label.setText(formatearTiempo(segundos[0]));
            }
        }));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
        timelinesActivos.add(tl);
    }

    private String formatearTiempo(long seg) {
        return String.format("Inicia en: %d:%02d", seg / 60, seg % 60);
    }

    // Modal de apuesta: load → pasar datos → showAndWait (orden obligatorio)
    private void abrirModalApostar(Carrera carrera, List<Caballo> caballos) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/taqueardeelestablo/view/modal-apostar.fxml")
            );
            Parent root = loader.load();

            ModalApostarController ctrl = loader.getController();
            ctrl.setDatos(carrera, caballos);   // datos ANTES de show

            Stage modal = new Stage();
            modal.setTitle("Apostar — Carrera #" + carrera.getIdCarrera());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(SceneManager.getPrimaryStage());
            modal.setScene(new Scene(root));
            modal.showAndWait();

            // Refresca saldo en cabecera tras apostar
            actualizarCabecera();
        } catch (IOException ex) {
            System.err.println("DashboardUsuario.abrirModalApostar: " + ex.getMessage());
        }
    }

    private void abrirVerCarrera(Carrera carrera) {
        FXMLLoader loader = SceneManager.abrirVentana(
                "ver-carrera.fxml", "Carrera #" + carrera.getIdCarrera()
        );
        if (loader != null) {
            VerCarreraController ctrl = loader.getController();
            ctrl.setCarrera(carrera);
        }
    }

    @FXML
    private void handleVerApuestasActivas() {
        SceneManager.abrirVentana("apuestas-activas.fxml", "Apuestas Activas");
    }

    @FXML
    private void handleDepositar() {
        SceneManager.abrirVentana("depositar.fxml", "Depositar");
    }

    @FXML
    private void handleRetirar() {
        SceneManager.abrirVentana("retirar.fxml", "Retirar");
    }

    @FXML
    private void handleHistorial() {
        SceneManager.abrirVentana("historial.fxml", "Historial de Apuestas");
    }

    @FXML
    private void handleCerrarSesion() {
        timelinesActivos.forEach(Timeline::stop);
        SessionManager.getInstance().cerrarSesion();
        SceneManager.cambiarEscena("login.fxml");
    }
}