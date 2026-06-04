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
import javafx.geometry.Insets;

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

    private VBox crearTarjetaCarrera(Carrera carrera, List<Caballo> caballos) {
        VBox tarjeta = new VBox(15);
        tarjeta.getStyleClass().add("card"); // Aplicamos el estilo del login
        tarjeta.setStyle("-fx-border-color: #FF6B00; -fx-border-width: 0 0 0 4;");

        // Stats Row
        double porcentaje = carrera.getNumCaballos() > 0 ? 100.0 / carrera.getNumCaballos() : 0;
        Label multLabel = new Label("Multiplicador: x" + carrera.getNumCaballos());
        multLabel.setStyle("-fx-text-fill: #F1C40F; -fx-font-weight: bold;");

        Label pctLabel = new Label(String.format("%%: %.2f", porcentaje));
        pctLabel.setStyle("-fx-text-fill: #95A5A6;");

        HBox statsRow = new HBox(20);
        statsRow.getChildren().addAll(multLabel, pctLabel);

        // Tabla
        TableView<Caballo> tabla = crearTablaCaballos(caballos);

        // Botones
        Button apostarBtn = new Button("APOSTAR 💸");
        apostarBtn.getStyleClass().add("button-primary");
        apostarBtn.setPrefSize(140, 40);

        Button verCarreraBtn = new Button("VER CARRERA 🎥");
        verCarreraBtn.getStyleClass().add("btn-history"); // Reutilizamos clase de botones secundarios
        verCarreraBtn.setPrefSize(140, 40);

        HBox botonesRow = new HBox(12);
        botonesRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        botonesRow.getChildren().addAll(apostarBtn, verCarreraBtn);

        // Eventos
        apostarBtn.setOnAction(e -> abrirModalApostar(carrera, caballos));
        verCarreraBtn.setOnAction(e -> abrirVerCarrera(carrera));

        tarjeta.getChildren().addAll(statsRow, tabla, botonesRow);
        return tarjeta;
    }

    private TableView<Caballo> crearTablaCaballos(List<Caballo> caballos) {
        TableView<Caballo> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(150);

        // Aplicamos la clase del CSS, el estilo ya no está "hardcodeado" aquí
        tabla.getStyleClass().add("table-view");

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
        LocalDateTime inicio = carrera.getFechaInicio();
        if(inicio == null){
            label.setText("Iniciando Pronto....");
            return;
        }
        label.setText(String.format("Inicia: %d:%02d", inicio.getHour(), inicio.getMinute()));


    }

    private String formatearTiempo(long seg) {
        return String.format("En: %d:%02d", seg / 60, seg % 60);
    }

    private void abrirModalApostar(Carrera carrera, List<Caballo> caballos) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/taqueardeelestablo/view/modal-apostar.fxml"));
            Parent root = loader.load();

            ModalApostarController controller = loader.getController();
            controller.setDatos(carrera, caballos); // ← ahora pasa los dos argumentos

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Apostar");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            actualizarCabecera(); // refresca el saldo al cerrar el modal
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirVerCarrera(Carrera carrera) {
        timelinesActivos.forEach(Timeline::stop); // limpia antes de salir
        FXMLLoader loader = SceneManager.cambiarEscena("ver-carrera.fxml");
        if (loader != null) {
            VerCarreraController ctrl = loader.getController();
            ctrl.setCarrera(carrera);
        }
    }

    @FXML
    private void handleCerrarSesion() {
        timelinesActivos.forEach(Timeline::stop);
        SessionManager.getInstance().cerrarSesion();
        SceneManager.cambiarEscena("login.fxml");
    }

    @FXML
    private void handleVerApuestasActivas() {
        timelinesActivos.forEach(Timeline::stop);
        SceneManager.cambiarEscena("apuestas-activas.fxml");
    }

    @FXML
    private void handleDepositar() {
        timelinesActivos.forEach(Timeline::stop);
        SceneManager.cambiarEscena("depositar.fxml");
    }

    @FXML
    private void handleRetirar() {
        timelinesActivos.forEach(Timeline::stop);
        SceneManager.cambiarEscena("retirar.fxml");
    }

    @FXML
    private void handleHistorial() {
        timelinesActivos.forEach(Timeline::stop);
        SceneManager.cambiarEscena("historial.fxml");
    }
}