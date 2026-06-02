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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrearCarreraController {

    @FXML private Label          adminLabel;
    @FXML private ComboBox<Integer> numCaballosCombo;
    @FXML private TextField      duracionField;
    @FXML private ToggleGroup    gateraGroup;
    @FXML private ListView<String> caballosListView;
    @FXML private Label          mensajeLabel;

    private final CaballoDAO       caballoDAO       = new CaballoDAO();
    private final CarreraDAO       carreraDAO       = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();

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
    }

    @FXML
    private void handleCargar() {
        int n = numCaballosCombo.getValue();
        List<Caballo> todos = caballoDAO.findAll();

        if (todos.size() < n) {
            mensajeLabel.setText("No hay suficientes caballos registrados. " +
                    "Se necesitan " + n + " y solo hay " + todos.size() + ".");
            caballosListView.getItems().clear();
            caballosSeleccionados.clear();
            return;
        }

        Collections.shuffle(todos);
        caballosSeleccionados = new ArrayList<>(todos.subList(0, n));

        // Muestra los caballos en la lista con formato "Nombre | No. número"
        List<String> filas = new ArrayList<>();
        for (int i = 0; i < caballosSeleccionados.size(); i++) {
            Caballo c = caballosSeleccionados.get(i);
            filas.add((i + 1) + ". " + c.getNombre() + " | No. " + c.getNumero());
        }
        caballosListView.setItems(FXCollections.observableArrayList(filas));
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

        // Regresa al menú con mensaje de éxito
        FXMLLoader loader = SceneManager.cambiarEscena("menu-admin.fxml");
        if (loader != null) {
            MenuAdminController ctrl = loader.getController();
            ctrl.setMensaje("Carrera #" + carrera.getIdCarrera() + " creada correctamente.");
        }
    }

    @FXML
    private void handleVolver() {
        SceneManager.cambiarEscena("menu-admin.fxml");
    }
}