package Controller;

import Controller.util.SessionManager;
import Model.Apuesta;
import Model.Caballo;
import Model.Usuario;
import Model.dao.ApuestaDAO;
import Model.dao.CaballoDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


import java.util.List;

public class HistorialController {

    @FXML private Label usuarioLabel;
    @FXML private Label saldoLabel;
    @FXML private VBox  historialContainer;

    private final ApuestaDAO apuestaDAO = new ApuestaDAO();
    private final CaballoDAO caballoDAO = new CaballoDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
        cargarHistorial();
    }

    private void cargarHistorial() {
        int idUsuario = SessionManager.getInstance().getUsuarioActual().getIdUsuario();
        List<Apuesta> apuestas = apuestaDAO.findByUsuario(idUsuario);

        if (apuestas.isEmpty()) {
            historialContainer.getChildren().add(
                    new Label("No tienes apuestas registradas.")
            );
            return;
        }

        for (Apuesta a : apuestas) {
            historialContainer.getChildren().add(crearTarjetaHistorial(a));
        }
    }

    private VBox crearTarjetaHistorial(Apuesta apuesta) {
        Caballo caballo = caballoDAO.findById(apuesta.getIdCaballo());
        String infoCaballo = caballo != null
                ? caballo.getNombre() + " — No. " + caballo.getNumero()
                : "Caballo #" + apuesta.getIdCaballo();

        double porcentaje = 100.0 / apuesta.getMultiplicador();

        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(5);

        grid.add(new Label("Caballo: " + infoCaballo),                                  0, 0, 2, 1);
        grid.add(new Label("Resultado: " + apuesta.getResultado().name()),               0, 1);
        grid.add(new Label("Carrera #" + apuesta.getIdCarrera()),                        1, 1);
        grid.add(new Label(String.format("Apuesta: %.2f", apuesta.getMonto())),          0, 2);
        grid.add(new Label(String.format("Cobro: %.2f", apuesta.getCobro())),            1, 2);

        Button imprimirBtn = new Button("Imprimir Tiket");
        imprimirBtn.setOnAction(e -> imprimirTiketApuesta(apuesta, caballo, porcentaje));

        VBox card = new VBox(6, grid, imprimirBtn);
        card.setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-padding: 8;");
        return card;
    }

    private void imprimirTiketApuesta(Apuesta apuesta, Caballo caballo, double porcentaje) {
        String nombre = caballo != null
                ? caballo.getNombre() + " | No. " + caballo.getNumero()
                : "Caballo #" + apuesta.getIdCaballo();

        String ticket = "=== TIKET DE APUESTA ===\n"
                + "Carrera:     #"  + apuesta.getIdCarrera()           + "\n"
                + "Caballo:      "  + nombre                            + "\n"
                + String.format("Porcentaje:   %.2f%%%n", porcentaje)
                + String.format("Apuesta:      %.2f%n",  apuesta.getMonto())
                + "Resultado:    "  + apuesta.getResultado().name()     + "\n"
                + String.format("Cobro:        %.2f%n",  apuesta.getCobro())
                + "========================";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tiket de Apuesta");
        alert.setHeaderText("Carrera #" + apuesta.getIdCarrera());
        alert.setContentText(ticket);
        alert.showAndWait();
    }

    @FXML
    private void handleVolver() {
        ((Stage) historialContainer.getScene().getWindow()).close();
    }
}