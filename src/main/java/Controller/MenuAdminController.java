package Controller;

import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Caballo;
import Model.Carrera;
import Model.CarreraCaballo;
import Model.dao.CaballoDAO;
import Model.dao.CarreraDAO;
import Model.dao.CarreraCaballoDAO;
import Model.enums.EstadoCarrera;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MenuAdminController {
    @FXML
    private Label adminLabel;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab inicioTab;
    @FXML
    private Label inicioMensajeLabel;
    @FXML
    private VBox carrerasAdminContainer;
    @FXML
    private Tab tabCrearCarrera;
    @FXML
    private Tab tabRegistrarCaballo;
    @FXML
    private Tab tabRegistrarAdmin;
    @FXML
    private Tab tabCaballos;

    private final CarreraDAO carreraDAO = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO caballoDAO = new CaballoDAO();

    @FXML
    private void initialize() {
        adminLabel.setText("Administrador: " + SessionManager.getInstancia().getUsuarioActual().getUsername());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taqueardeelestablo/view/crear-carrera.fxml"));
            tabCrearCarrera.setContent(loader.load());
            CrearCarreraController ctrl = loader.getController();
            ctrl.setMenuController(this);
        } catch (IOException e) {
            System.err.println("Error al cargar crear-carrera.fxml: " + e.getMessage());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taqueardeelestablo/view/registrar-caballo.fxml"));
            tabRegistrarCaballo.setContent(loader.load());
            RegistrarCaballoController ctrl = loader.getController();
            ctrl.setMenuController(this);
        } catch (IOException e) {
            System.err.println("Error al cargar registrar-caballo.fxml: " + e.getMessage());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taqueardeelestablo/view/registrar-admin.fxml"));
            tabRegistrarAdmin.setContent(loader.load());
            RegistrarAdminController ctrl = loader.getController();
            ctrl.setMenuController(this);
        } catch (IOException e) {
            System.err.println("Error al cargar registrar-admin.fxml: " + e.getMessage());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taqueardeelestablo/view/caballos-registrados.fxml"));
            tabCaballos.setContent(loader.load());
            CaballosRegistradosController ctrl = loader.getController();
            ctrl.setMenuController(this);
        } catch (IOException e) {
            System.err.println("Error al cargar caballos-registrados.fxml: " + e.getMessage());
        }

        inicioTab.setOnSelectionChanged(e -> {
            if (inicioTab.isSelected()) cargarCarrerasActivas();
        });

        cargarCarrerasActivas();
    }

    public void mostrarMensajeEnInicio(String mensaje) {
        inicioMensajeLabel.setText(mensaje);
        irAInicio();
    }

    public void irAInicio() {
        tabPane.getSelectionModel().select(inicioTab);
        cargarCarrerasActivas();
    }

    private void cargarCarrerasActivas() {
        carrerasAdminContainer.getChildren().clear();
        List<Carrera> carreras = carreraDAO.buscarActivas();

        if (carreras.isEmpty()) {
            carrerasAdminContainer.getChildren().add(
                    new Label("No hay carreras activas en este momento.")
            );
            return;
        }

        for (Carrera c : carreras) {
            List<Caballo> caballos = getCaballosDeCarrera(c);
            carrerasAdminContainer.getChildren().add(crearTarjetaAdmin(c, caballos));
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

    private VBox crearTarjetaAdmin(Carrera carrera, List<Caballo> caballos) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        String estadoTexto = carrera.getEstado() == EstadoCarrera.EN_GATERA ? "En Espera" : "En Progreso";

        Label headerLabel = new Label("Carrera #" + carrera.getIdCarrera() + " — " + estadoTexto + " | Duración: " + carrera.getDuracionSeg() + " seg");
        headerLabel.getStyleClass().add("section-title");

        // Calcula y muestra la hora de inicio
        String horaInicio = "—";
        if (carrera.getFechaCreacion() != null) {
            LocalDateTime inicio = carrera.getFechaCreacion().plusMinutes(carrera.getTiempoGatera());
            horaInicio = inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }
        Label inicioLabel = new Label("Inicio: " + horaInicio);
        inicioLabel.getStyleClass().add("header-subtitle");

        TableView<Caballo> tabla = crearTablaCaballos(caballos);
        tabla.getStyleClass().add("table-view");

        card.getChildren().addAll(headerLabel, inicioLabel, tabla);
        return card;
    }

    // Construye la tabla de caballos de una tarjeta
    private TableView<Caballo> crearTablaCaballos(List<Caballo> caballos) {
        TableView<Caballo> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(160);

        TableColumn<Caballo, String> nombreCol = new TableColumn<>("Nombre");
        nombreCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Caballo, Number> numeroCol = new TableColumn<>("Numero");
        numeroCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getNumero()));

        TableColumn<Caballo, Number> corridasCol = new TableColumn<>("Carreras Corridas");
        corridasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasCorridas()));

        TableColumn<Caballo, Number> ganadasCol = new TableColumn<>("Carreras Ganadas");
        ganadasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasGanadas()));

        tabla.getColumns().addAll(nombreCol, numeroCol, corridasCol, ganadasCol);
        tabla.setItems(FXCollections.observableArrayList(caballos));
        return tabla;
    }

    @FXML
    private void handleCerrarSesion() {
        SessionManager.getInstancia().cerrarSesion();
        SceneManager.cambiarEscena("login.fxml");
    }
}
