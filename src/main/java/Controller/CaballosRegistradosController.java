package Controller;

import Controller.util.SessionManager;
import Model.Caballo;
import Model.dao.CaballoDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CaballosRegistradosController {
    @FXML private Label adminLabel;
    @FXML private TableView<Caballo> caballosTable;
    @FXML private TextField buscarIdField;
    @FXML private TextField buscarNombreField;

    private final CaballoDAO caballoDAO = new CaballoDAO();
    private MenuAdminController menuController;

    @FXML
    private void initialize() {
        adminLabel.setText("Administrador: " + SessionManager.getInstancia().getUsuarioActual().getUsername());
        configurarTabla();
        cargarTodos();
    }

    public void setMenuController(MenuAdminController ctrl) {
        this.menuController = ctrl;
    }

    // Crea todas las columnas de la tabla, incluyendo los botones de acción
    private void configurarTabla() {
        TableColumn<Caballo, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getIdCaballo()));
        idCol.setPrefWidth(50);

        TableColumn<Caballo, String> nombreCol = new TableColumn<>("Nombre");
        nombreCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        nombreCol.setPrefWidth(140);

        TableColumn<Caballo, Number> numeroCol = new TableColumn<>("Numero");
        numeroCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getNumero()));
        numeroCol.setPrefWidth(80);

        TableColumn<Caballo, Number> corridasCol = new TableColumn<>("Carreras Corridas");
        corridasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasCorridas()));
        corridasCol.setPrefWidth(140);

        TableColumn<Caballo, Number> ganadasCol = new TableColumn<>("Carreras Ganadas");
        ganadasCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCarrerasGanadas()));
        ganadasCol.setPrefWidth(140);

        // Columna con botón Actualizar
        TableColumn<Caballo, Void> actualizarCol = new TableColumn<>("Actualizar");
        actualizarCol.setPrefWidth(110);
        actualizarCol.setCellFactory(c -> new TableCell<Caballo, Void>() {
            private final Button btn = new Button("Actualizar");
            {
                btn.setOnAction(e -> {
                    Caballo caballo = getTableView().getItems().get(getIndex());
                    abrirModalActualizar(caballo);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Columna con botón Eliminar
        TableColumn<Caballo, Void> eliminarCol = new TableColumn<>("Eliminar");
        eliminarCol.setPrefWidth(110);
        eliminarCol.setCellFactory(c -> new TableCell<Caballo, Void>() {
            private final Button btn = new Button("Eliminar");
            {
                btn.setOnAction(e -> {
                    Caballo caballo = getTableView().getItems().get(getIndex());
                    confirmarEliminar(caballo);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        caballosTable.getColumns().addAll(
                idCol, nombreCol, numeroCol, corridasCol, ganadasCol,
                actualizarCol, eliminarCol
        );
    }

    private void cargarTodos() {
        cargarLista(caballoDAO.findAll());
    }

    private void cargarLista(List<Caballo> lista) {
        caballosTable.setItems(FXCollections.observableArrayList(lista));
    }

    @FXML
    private void handleBuscar() {
        String id = buscarIdField.getText().trim();
        String nombre = buscarNombreField.getText().trim();

        if (id.isEmpty() && nombre.isEmpty()) {
            cargarTodos();
            return;
        }

        List<Caballo> resultado = caballoDAO.buscar(nombre, id);
        cargarLista(resultado);

        if (resultado.isEmpty()) {
            mostrarInfo("Sin resultados", "No se encontró ningún caballo con esos criterios.");
        }
    }

    @FXML
    private void handleLimpiar() {
        buscarIdField.clear();
        buscarNombreField.clear();
        cargarTodos();
    }

    private void abrirModalActualizar(Caballo caballo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taqueardeelestablo/view/actualizar-caballo.fxml"));
            Parent root = loader.load();

            ActualizarCaballoController ctrl = loader.getController();
            ctrl.setCaballo(caballo);

            Stage modal = new Stage();
            modal.setTitle("Actualizar Caballo");
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner((Stage) caballosTable.getScene().getWindow());
            modal.setScene(new Scene(root));
            modal.showAndWait();

            // Refresca la tabla al cerrar el modal
            cargarTodos();
        } catch (IOException e) {
            System.err.println("CaballosRegistrados.abrirModalActualizar: " + e.getMessage());
        }
    }

    private void confirmarEliminar(Caballo caballo) {
        // No se puede eliminar si el caballo está en una carrera activa
        if (caballoDAO.estaEnCarreraActiva(caballo.getIdCaballo())) {
            mostrarError("No se puede eliminar", "\"" + caballo.getNombre() + "\" está en una carrera pendiente o en curso.\n" + "Espera a que finalice e intenta de nuevo.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar Caballo");
        confirm.setHeaderText("¿Estás seguro de eliminar a \"" + caballo.getNombre() + "\"?");
        confirm.setContentText("Se eliminará también su historial de carreras y apuestas.\n" + "Esta acción no se puede deshacer."
        );
        confirm.initOwner((Stage) caballosTable.getScene().getWindow());

        ButtonType btnConfirmar = new ButtonType("Confirmar");
        ButtonType btnCancelar  = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnConfirmar, btnCancelar);

        Optional<ButtonType> respuesta = confirm.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == btnConfirmar) {
            if (caballoDAO.deleteConCascada(caballo.getIdCaballo())) {
                cargarTodos();
            } else {
                mostrarError("Error", "No se pudo eliminar el caballo.");
            }
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.initOwner((Stage) caballosTable.getScene().getWindow());
        a.showAndWait();
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.initOwner((Stage) caballosTable.getScene().getWindow());
        a.showAndWait();
    }

    @FXML
    private void handleVolver() {
        if (menuController != null) menuController.irAInicio();
    }
}
