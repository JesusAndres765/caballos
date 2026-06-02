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
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public class ApuestasActivasController {

    @FXML private Label usuarioLabel;
    @FXML private Label saldoLabel;
    @FXML private VBox  apuestasContainer;

    private final ApuestaDAO apuestaDAO = new ApuestaDAO();
    private final CaballoDAO caballoDAO = new CaballoDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
        cargarApuestas();
    }

    private void cargarApuestas() {
        apuestasContainer.getChildren().clear();

        int idUsuario = SessionManager.getInstance().getUsuarioActual().getIdUsuario();
        List<Apuesta> todas  = apuestaDAO.findByUsuario(idUsuario);

        List<Apuesta> activas = todas.stream()
                .filter(a -> a.getResultado() == ResultadoApuesta.PENDIENTE)
                .collect(Collectors.toList());

        if (activas.isEmpty()) {
            apuestasContainer.getChildren().add(new Label("No tienes apuestas activas."));
            return;
        }

        apuestasContainer.getChildren().add(
                new Label("No. de Apuestas: " + activas.size())
        );

        for (Apuesta a : activas) {
            apuestasContainer.getChildren().add(crearTarjetaApuesta(a));
        }
    }

    private VBox crearTarjetaApuesta(Apuesta apuesta) {
        Caballo caballo = caballoDAO.findById(apuesta.getIdCaballo());
        String infoCaballo = caballo != null
                ? caballo.getNombre() + " — No. " + caballo.getNumero()
                : "Caballo #" + apuesta.getIdCaballo();

        double porcentaje    = 100.0 / apuesta.getMultiplicador();
        double premioEsperad = apuesta.getMonto() * apuesta.getMultiplicador();

        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(5);

        grid.add(new Label("Caballo: " + infoCaballo),                              0, 0, 2, 1);
        grid.add(new Label(String.format("%%: %.2f", porcentaje)),                   0, 1);
        grid.add(new Label(String.format("Apuesta: %.2f", apuesta.getMonto())),      1, 1);
        grid.add(new Label("Carrera #" + apuesta.getIdCarrera()),                    0, 2);
        grid.add(new Label(String.format("Premio Esperado: %.2f", premioEsperad)),   1, 2);

        VBox card = new VBox(grid);
        card.setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-padding: 8;");
        return card;
    }

    @FXML
    private void handleVolver() {
        SceneManager.cambiarEscena("dashboard-usuario.fxml");
    }
}