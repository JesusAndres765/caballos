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

    @FXML private Label    adminLabel;
    @FXML private TabPane  tabPane;
    @FXML private Tab      inicioTab;
    @FXML private Label    inicioMensajeLabel;
    @FXML private VBox     carrerasAdminContainer;
    @FXML private Tab      tabCrearCarrera;
    @FXML private Tab      tabRegistrarCaballo;
    @FXML private Tab      tabRegistrarAdmin;
    @FXML private Tab      tabCaballos;

    private final CarreraDAO        carreraDAO        = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO        caballoDAO        = new CaballoDAO();

    @FXML
    private void initialize() {
        adminLabel.setText("Administrador: " +
                SessionManager.getInstance().getUsuarioActual().getUsername());

        // Carga el FXML de cada tab y pasa la referencia de este controlador
        cargarEnTab(tabCrearCarrera,     "crear-carrera.fxml",      loader ->
                ((CrearCarreraController)     loader.getController()).setMenuController(this));

        cargarEnTab(tabRegistrarCaballo, "registrar-caballo.fxml",  loader ->
                ((RegistrarCaballoController) loader.getController()).setMenuController(this));

        cargarEnTab(tabRegistrarAdmin,   "registrar-admin.fxml",    loader ->
                ((RegistrarAdminController)   loader.getController()).setMenuController(this));

        cargarEnTab(tabCaballos,         "caballos-registrados.fxml", loader ->
                ((CaballosRegistradosController) loader.getController()).setMenuController(this));

        // Refresca el dashboard de Inicio cada vez que se selecciona esa tab
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, actual) -> {
                    if (actual == inicioTab) cargarCarrerasActivas();
                }
        );

        // Carga inicial
        cargarCarrerasActivas();
    }

    // ── API pública para sub-controladores ───────────────────────────────────

    // Muestra un mensaje en Inicio y vuelve a esa pestaña
    public void mostrarMensajeEnInicio(String mensaje) {
        inicioMensajeLabel.setText(mensaje);
        irAInicio();
    }

    // Cambia a la tab Inicio y refresca la lista de carreras
    public void irAInicio() {
        tabPane.getSelectionModel().select(inicioTab);
        cargarCarrerasActivas();
    }

    // ── Dashboard de carreras en Inicio ──────────────────────────────────────

    private void cargarCarrerasActivas() {
        carrerasAdminContainer.getChildren().clear();
        List<Carrera> carreras = carreraDAO.findActivas();

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
        List<CarreraCaballo> inscripciones =
                carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
        List<Caballo> lista = new ArrayList<>();
        for (CarreraCaballo cc : inscripciones) {
            Caballo c = caballoDAO.findById(cc.getIdCaballo());
            if (c != null) lista.add(c);
        }
        return lista;
    }

    private VBox crearTarjetaAdmin(Carrera carrera, List<Caballo> caballos) {
        VBox card = new VBox(6);
        card.setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-padding: 10;");

        // Estado en texto legible
        String estadoTexto = carrera.getEstado() == EstadoCarrera.EN_GATERA
                ? "En Espera" : "En Progreso";
        Label headerLabel = new Label(
                "Carrera #" + carrera.getIdCarrera() + " — " + estadoTexto
                        + " | Duración: " + carrera.getDuracionSeg() + " seg"
        );

        // Hora de inicio
        String horaInicio = "—";
        if (carrera.getFechaCreacion() != null) {
            LocalDateTime inicio = carrera.getFechaCreacion()
                    .plusMinutes(carrera.getTiempoGatera());
            horaInicio = inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }
        Label inicioLabel = new Label("Inicio: " + horaInicio);

        // Tabla de caballos
        TableView<Caballo> tabla = crearTablaCaballos(caballos);

        card.getChildren().addAll(headerLabel, inicioLabel, tabla);
        return card;
    }

    private TableView<Caballo> crearTablaCaballos(List<Caballo> caballos) {
        TableView<Caballo> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(160);

        TableColumn<Caballo, String> nombreCol = new TableColumn<>("Nombre");
        nombreCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Caballo, Number> numeroCol = new TableColumn<>("Numero");
        numeroCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getNumero()));

        TableColumn<Caballo, Number> corridasCol = new TableColumn<>("Carreras Corridas");
        corridasCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getCarrerasCorridas()));

        TableColumn<Caballo, Number> ganadasCol = new TableColumn<>("Carreras Ganadas");
        ganadasCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getCarrerasGanadas()));

        tabla.getColumns().addAll(nombreCol, numeroCol, corridasCol, ganadasCol);
        tabla.setItems(FXCollections.observableArrayList(caballos));
        return tabla;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Carga un FXML y lo establece como contenido del tab dado
    @FunctionalInterface
    interface LoaderConsumer { void accept(FXMLLoader loader); }

    private void cargarEnTab(Tab tab, String fxmlFile, LoaderConsumer setup) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/taqueardeelestablo/view/" + fxmlFile)
            );
            tab.setContent(loader.load());
            if (setup != null) setup.accept(loader);
        } catch (IOException e) {
            System.err.println("MenuAdmin.cargarEnTab [" + fxmlFile + "]: " + e.getMessage());
        }
    }

    // ── Sesión ────────────────────────────────────────────────────────────────

    @FXML
    private void handleCerrarSesion() {
        SessionManager.getInstance().cerrarSesion();
        SceneManager.cambiarEscena("login.fxml");
    }
}