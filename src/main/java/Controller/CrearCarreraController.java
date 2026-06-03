package Controller;

import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Caballo;
import Model.Carrera;
import Model.CarreraCaballo;
import Model.dao.CaballoDAO;
import Model.dao.CarreraCaballoDAO;
import Model.dao.CarreraDAO;
import Model.enums.EstadoCarrera;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrearCarreraController {

    @FXML private Label          adminLabel;
    @FXML private ComboBox<Integer> numCaballosCombo;
    @FXML private TextField      duracionField;
    @FXML private ToggleGroup    gateraGroup;
    @FXML private TableView<Caballo> caballosTable;
    @FXML private Label          mensajeLabel;

    private final CaballoDAO       caballoDAO       = new CaballoDAO();
    private final CarreraDAO       carreraDAO       = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private MenuAdminController menuController;

    // Guarda los caballos seleccionados al cargar para usarlos al crear la carrera
    private List<Caballo> caballosSeleccionados = new ArrayList<>();

    @FXML
    private void initialize() {
        String username = SessionManager.getInstance().getUsuarioActual().getUsername();
        adminLabel.setText("Administrador: " + username);

        // Rellena el ComboBox con opciones 2 a 10
        List<Integer> opciones = new ArrayList<>();
        for (int i = 2; i <= 10; i++) opciones.add(i);
        numCaballosCombo.setItems(FXCollections.observableArrayList(opciones));
        numCaballosCombo.setValue(2);

        configurarTabla();
    }

    // Método nuevo a agregar en la clase
    private void configurarTabla() {
        TableColumn<Caballo, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getIdCaballo()));

        TableColumn<Caballo, String> nombreCol = new TableColumn<>("Nombre");
        nombreCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Caballo, Number> numeroCol = new TableColumn<>("Numero");
        numeroCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getNumero()));

        TableColumn<Caballo, Number> corridasCol = new TableColumn<>("Carreras Corridas");
        corridasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasCorridas()));

        TableColumn<Caballo, Number> ganadasCol = new TableColumn<>("Carreras Ganadas");
        ganadasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasGanadas()));

        caballosTable.getColumns().addAll(idCol, nombreCol, numeroCol, corridasCol, ganadasCol);
        caballosTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void setMenuController(MenuAdminController ctrl) {
        this.menuController = ctrl;
    }

    @FXML
    private void handleCargar() {
        int n = numCaballosCombo.getValue();
        List<Caballo> todos = caballoDAO.findAll();

        if (todos.size() < n) {
            mensajeLabel.setText("No hay suficientes caballos registrados. " +
                    "Se necesitan " + n + " y solo hay " + todos.size() + ".");
            caballosTable.getItems().clear();
            caballosSeleccionados.clear();
            return;
        }

        Collections.shuffle(todos);
        caballosSeleccionados = new ArrayList<>(todos.subList(0, n));
        caballosTable.setItems(FXCollections.observableArrayList(caballosSeleccionados));
        mensajeLabel.setText("");
    }

    @FXML
    private void handleCrearCarrera() {
        // Validación: caballos cargados
        if (caballosSeleccionados.isEmpty()) {
            mensajeLabel.setText("Primero carga los caballos.");
            return;
        }

        // Validación: duración
        String duracionTexto = duracionField.getText().trim();
        int duracion;
        try {
            duracion = Integer.parseInt(duracionTexto);
        } catch (NumberFormatException e) {
            mensajeLabel.setText("La duración debe ser un número entero.");
            return;
        }
        if (duracion < 30 || duracion > 120) {
            mensajeLabel.setText("La duración debe estar entre 30 y 120 segundos.");
            return;
        }

        // Validación: tiempo de gatera seleccionado
        Toggle toggleSeleccionado = gateraGroup.getSelectedToggle();
        if (toggleSeleccionado == null) {
            mensajeLabel.setText("Selecciona el tiempo de gatera.");
            return;
        }
        int tiempoGatera = Integer.parseInt((String) toggleSeleccionado.getUserData());

        // Construye y persiste la carrera
        Carrera carrera = new Carrera();
        carrera.setIdAdmin(SessionManager.getInstance().getUsuarioActual().getIdUsuario());
        carrera.setNumCaballos(caballosSeleccionados.size());
        carrera.setDuracionSeg(duracion);
        carrera.setTiempoGatera(tiempoGatera);
        carrera.setEstado(EstadoCarrera.EN_GATERA);
        carrera.setFechaInicio(LocalDateTime.now().plusMinutes(tiempoGatera));

        if (!carreraDAO.insert(carrera)) {
            mensajeLabel.setText("Error al guardar la carrera.");
            return;
        }

        // Inscribe cada caballo a la carrera recién creada
        for (Caballo caballo : caballosSeleccionados) {
            CarreraCaballo cc = new CarreraCaballo();
            cc.setIdCarrera(carrera.getIdCarrera());
            cc.setIdCaballo(caballo.getIdCaballo());
            carreraCaballoDAO.insert(cc);
        }

        menuController.mostrarMensajeEnInicio(
                "Carrera #" + carrera.getIdCarrera() + " creada correctamente."
        );
    }

    @FXML
    private void handleVolver() {
        if (menuController != null) menuController.irAInicio();
    }
}