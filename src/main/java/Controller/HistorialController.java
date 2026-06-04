package Controller;

import Controller.util.SceneManager;
import Controller.util.SessionManager;
import Model.Apuesta;
import Model.Caballo;
import Model.Usuario;
import Model.dao.ApuestaDAO;
import Model.dao.CaballoDAO;
import Model.enums.ResultadoApuesta;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class HistorialController {
    @FXML private Label usuarioLabel;
    @FXML private Label saldoLabel;
    @FXML private VBox  historialContainer;

    private final ApuestaDAO apuestaDAO = new ApuestaDAO();
    private final CaballoDAO caballoDAO = new CaballoDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstancia().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
        cargarHistorial();
    }

    private void cargarHistorial() {
        int idUsuario = SessionManager.getInstancia().getUsuarioActual().getIdUsuario();
        List<Apuesta> apuestas = apuestaDAO.findByUsuario(idUsuario);

        if (apuestas.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(javafx.geometry.Pos.CENTER);
            empty.setStyle("-fx-padding: 40 0 0 0;");

            Label ico = new Label("");
            ico.setStyle("-fx-font-size: 32px;");

            Label msg = new Label("No tienes apuestas registradas.");
            msg.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13px;");

            empty.getChildren().addAll(ico, msg);
            historialContainer.getChildren().add(empty);
            return;
        }

        for (Apuesta a : apuestas) {
            historialContainer.getChildren().add(crearTarjetaHistorial(a));
        }
    }

    private VBox crearTarjetaHistorial(Apuesta apuesta) {
        Caballo caballo    = caballoDAO.findById(apuesta.getIdCaballo());
        String infoCaballo = caballo != null
                ? caballo.getNombre() + "  —  No. " + caballo.getNumero()
                : "Caballo #" + apuesta.getIdCaballo();
        double porcentaje  = 100.0 / apuesta.getMultiplicador();

        String badgeColor;
        if (apuesta.getResultado() == ResultadoApuesta.GANADA) {
            badgeColor = "#2ECC71";
        } else if (apuesta.getResultado() == ResultadoApuesta.PERDIDA) {
            badgeColor = "#E74C3C";
        } else {
            badgeColor = "#F39C12";
        }

        Label badge = new Label(apuesta.getResultado().name());
        badge.setStyle("-fx-background-color: " + badgeColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 10px;" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 3 8 3 8;");

        Label nombreLabel = new Label(infoCaballo);
        nombreLabel.setStyle("-fx-text-fill: #ECEFF1; -fx-font-weight: bold; -fx-font-size: 13px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerRow = new HBox(10, nombreLabel, spacer, badge);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2C313D;");

        Label carreraLabel = new Label("Carrera #" + apuesta.getIdCarrera());
        carreraLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");

        Label apuestaLabel = new Label(String.format("Apostado: $%.2f", apuesta.getMonto()));
        apuestaLabel.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 12px;");

        String cobroColor = apuesta.getCobro() > 0 ? "#2ECC71" : "#7F8C8D";
        Label cobroLabel  = new Label(String.format("Cobro: $%.2f", apuesta.getCobro()));
        cobroLabel.setStyle("-fx-text-fill: " + cobroColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        HBox statsRow = new HBox(24, carreraLabel, apuestaLabel, cobroLabel);
        statsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button imprimirBtn = new Button("Imprimir Tiket");
        imprimirBtn.setStyle("-fx-background-color: #2C313D;" +
                "-fx-text-fill: #BDC3C7;" +
                "-fx-font-size: 11px;" +
                "-fx-background-radius: 5;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 5 14 5 14;");
        imprimirBtn.setOnAction(e -> imprimirTiket(apuesta, caballo, porcentaje));

        VBox card = new VBox(10, headerRow, sep, statsRow, imprimirBtn);
        card.setStyle("-fx-background-color: #1E222B;" +
                "-fx-border-color: #FF6B00 transparent transparent transparent;" +
                "-fx-border-width: 0 0 0 4;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 12 16 12 16;");
        return card;
    }

    // muestra el tiket
    private void imprimirTiket(Apuesta apuesta, Caballo caballo, double porcentaje) {
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
        SceneManager.cambiarEscena("dashboard-usuario.fxml");
    }
}
