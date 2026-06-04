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
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DashboardUsuarioController {

    @FXML private Label usuarioLabel;
    @FXML private Label saldoLabel;
    @FXML private VBox  carrerasContainer;

    private final CarreraDAO carreraDAO = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO caballoDAO  = new CaballoDAO();

    @FXML
    private void initialize() {
        actualizarCabecera();
        cargarCarreras();
    }

    // Muestra el nombre y saldo del usuario logueado
    private void actualizarCabecera() {
        Usuario u = SessionManager.getInstancia().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
    }

    // Carga y muestra todas las carreras activas en el panel principal
    private void cargarCarreras() {
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
        tarjeta.getStyleClass().add("card");
        tarjeta.setStyle("-fx-border-color: #FF6B00; -fx-border-width: 0 0 0 4;");

        double porcentaje = carrera.getNumCaballos() > 0 ? 100.0 / carrera.getNumCaballos() : 0;

        Label multLabel = new Label("Multiplicador: x" + carrera.getNumCaballos());
        multLabel.setStyle("-fx-text-fill: #F1C40F; -fx-font-weight: bold;");

        Label pctLabel = new Label(String.format("%%: %.2f", porcentaje));
        pctLabel.setStyle("-fx-text-fill: #95A5A6;");

        HBox statsRow = new HBox(20);
        statsRow.getChildren().addAll(multLabel, pctLabel);

        TableView<Caballo> tabla = crearTablaCaballos(caballos);

        Button apostarBtn = new Button("APOSTAR");
        apostarBtn.getStyleClass().add("button-primary");
        apostarBtn.setPrefSize(140, 40);

        Button verCarreraBtn = new Button("VER CARRERA");
        verCarreraBtn.getStyleClass().add("btn-history");
        verCarreraBtn.setPrefSize(140, 40);

        apostarBtn.setOnAction(e -> abrirModalApostar(carrera, caballos));
        verCarreraBtn.setOnAction(e -> abrirVerCarrera(carrera));

        HBox botonesRow = new HBox(12);
        botonesRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        botonesRow.getChildren().addAll(apostarBtn, verCarreraBtn);

        tarjeta.getChildren().addAll(statsRow, tabla, botonesRow);
        return tarjeta;
    }

    // Construye la tabla de caballos de una carrera
    private TableView<Caballo> crearTablaCaballos(List<Caballo> caballos) {
        TableView<Caballo> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(150);
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

    // Abre el modal para hacer una apuesta en la carrera seleccionada
    private void abrirModalApostar(Carrera carrera, List<Caballo> caballos) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/taqueardeelestablo/view/modal-apostar.fxml"));
            Parent root = loader.load();

            ModalApostarController ctrl = loader.getController();
            ctrl.setDatos(carrera, caballos);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Apostar");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            actualizarCabecera();
        } catch (IOException e) {
            System.err.println("DashboardUsuario.abrirModalApostar: " + e.getMessage());
        }
    }

    //Abre la ventana de la carrera
    private void abrirVerCarrera(Carrera carrera) {
        FXMLLoader loader = SceneManager.cambiarEscena("ver-carrera.fxml");
        if (loader != null) {
            VerCarreraController ctrl = loader.getController();
            ctrl.setCarrera(carrera);
        }
    }

    @FXML
    private void handleCerrarSesion() { SessionManager.getInstancia().cerrarSesion();
        SceneManager.cambiarEscena("login.fxml"); }

    @FXML
    private void handleVerApuestasActivas() {
        SceneManager.cambiarEscena("apuestas-activas.fxml");
    }

    @FXML
    private void handleDepositar() {
        SceneManager.cambiarEscena("depositar.fxml");
    }

    @FXML
    private void handleRetirar() {
        SceneManager.cambiarEscena("retirar.fxml");
    }

    @FXML
    private void handleHistorial() {
        SceneManager.cambiarEscena("historial.fxml");
    }
}
