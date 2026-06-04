package Controller;

import Controller.util.SessionManager;
import Model.Apuesta;
import Model.Caballo;
import Model.Carrera;
import Model.CarreraCaballo;
import Model.Usuario;
import Model.dao.ApuestaDAO;
import Model.dao.CaballoDAO;
import Model.dao.CarreraCaballoDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import Controller.util.SceneManager;

import java.util.ArrayList;
import java.util.List;

public class ResultadosController {

    @FXML private Label usuarioLabel;
    @FXML private Label saldoLabel;
    @FXML private VBox  resultadosContainer;
    @FXML private VBox  apuestasResultContainer;

    private Carrera        carrera;
    private List<Apuesta>  apuestasUsuario = new ArrayList<>();

    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO        caballoDAO        = new CaballoDAO();
    private final ApuestaDAO        apuestaDAO        = new ApuestaDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
    }

    // Llamado desde VerCarreraController.handleVerResultados()
    public void setCarrera(Carrera carrera) {
        this.carrera = carrera;
        cargarResultadosCarrera();
        cargarApuestasUsuario();
    }

    // Muestra la clasificación final ordenada por posicion_final
    private void cargarResultadosCarrera() {
        List<CarreraCaballo> inscripciones = carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
        inscripciones.sort((a, b) -> Integer.compare(a.getPosicionFinal(), b.getPosicionFinal()));

        for (CarreraCaballo cc : inscripciones) {
            Caballo c = caballoDAO.findById(cc.getIdCaballo());
            if (c == null) continue;

            String texto = cc.getPosicionFinal() + ". "
                    + c.getNombre() + " | No. " + c.getNumero();
            if (!cc.isTerminoCarrera()) texto += " (No Terminó la Carrera)";

            Label lbl = new Label(texto);
            lbl.setStyle("-fx-text-fill: #ECEFF1; -fx-font-size: 13px;");
            resultadosContainer.getChildren().add(lbl);

        }
    }

    // Muestra el veredicto de cada apuesta del usuario en esta carrera
    private void cargarApuestasUsuario() {
        int idUsuario = SessionManager.getInstance().getUsuarioActual().getIdUsuario();
        apuestasUsuario = apuestaDAO.findByUsuarioYCarrera(idUsuario, carrera.getIdCarrera());

        if (apuestasUsuario.isEmpty()) {
            apuestasResultContainer.getChildren().add(
                    new Label("No realizaste apuestas en esta carrera.")
            );
            return;
        }

        for (Apuesta a : apuestasUsuario) {
            Caballo c = caballoDAO.findById(a.getIdCaballo());
            String nombreCaballo = c != null
                    ? c.getNombre() + " | No. " + c.getNumero()
                    : "Caballo #" + a.getIdCaballo();

            VBox card = new VBox(4);
            card.setStyle("-fx-background-color: #1E222B; " +
                    "-fx-border-color: #3D4454; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 6; " +
                    "-fx-background-radius: 6; " +
                    "-fx-padding: 10;");
            card.getChildren().addAll(
                    crearLabel("Resultado: " + a.getResultado().name()),
                    crearLabel("Caballo: " + nombreCaballo),
                    crearLabel("Apuesta Inicial: " + String.format("%.2f", a.getMonto())),
                    crearLabel("Cobras: " + String.format("%.2f", a.getCobro()))
            );
            apuestasResultContainer.getChildren().add(card);
        }
    }

    private Label crearLabel(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-text-fill: #ECEFF1; -fx-font-size: 12px;");
        return lbl;
    }

    @FXML
    private void handleImprimirTiket() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TIKET — CARRERA #").append(carrera.getIdCarrera()).append(" ===\n\n");

        // Posiciones
        sb.append("POSICIONES FINALES:\n");
        List<CarreraCaballo> inscripciones = carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());
        inscripciones.sort((a, b) -> Integer.compare(a.getPosicionFinal(), b.getPosicionFinal()));

        for (CarreraCaballo cc : inscripciones) {
            Caballo c = caballoDAO.findById(cc.getIdCaballo());
            if (c == null) continue;
            String linea = cc.getPosicionFinal() + ". " + c.getNombre() + " | No. " + c.getNumero();
            if (!cc.isTerminoCarrera()) linea += " (No Terminó)";
            sb.append(linea).append("\n");
        }

        // Apuestas del usuario
        sb.append("\nMIS APUESTAS:\n");
        for (Apuesta a : apuestasUsuario) {
            Caballo c = caballoDAO.findById(a.getIdCaballo());
            String nombre = c != null ? c.getNombre() : "Caballo #" + a.getIdCaballo();
            sb.append("Caballo: ").append(nombre).append("\n");
            sb.append("Resultado: ").append(a.getResultado().name()).append("\n");
            sb.append(String.format("Apuesta:  %.2f%n", a.getMonto()));
            sb.append(String.format("Cobro:    %.2f%n%n", a.getCobro()));
        }
        sb.append("================================");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tiket de Apuesta");
        alert.setHeaderText("Carrera #" + carrera.getIdCarrera());
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleVolver() {
        SceneManager.cambiarEscena("dashboard-usuario.fxml");
    }
}