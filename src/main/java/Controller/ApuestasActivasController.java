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
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ApuestasActivasController {
    @FXML private Label usuarioLabel;
    @FXML private Label saldoLabel;
    @FXML private VBox apuestasContainer;

    private final ApuestaDAO apuestaDAO = new ApuestaDAO();
    private final CaballoDAO caballoDAO = new CaballoDAO();

    @FXML
    private void initialize() {
        Usuario u = SessionManager.getInstancia().getUsuarioActual();
        usuarioLabel.setText("Usuario: " + u.getUsername());
        saldoLabel.setText(String.format("Saldo: %.2f", u.getSaldo()));
        cargarApuestas();
    }

    private void cargarApuestas() {
        apuestasContainer.getChildren().clear();

        int idUsuario = SessionManager.getInstancia().getUsuarioActual().getIdUsuario();
        List<Apuesta> todas = apuestaDAO.findByUsuario(idUsuario);

        List<Apuesta> activas = new ArrayList<>();
        for (Apuesta a : todas) {
            if (a.getResultado() == ResultadoApuesta.PENDIENTE) {
                activas.add(a);
            }
        }

        if (activas.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(javafx.geometry.Pos.CENTER);
            empty.setStyle("-fx-padding: 40 0 0 0;");

            Label ico = new Label("");
            ico.setStyle("-fx-font-size: 32px;");

            Label msg = new Label("No tienes apuestas activas.");
            msg.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13px;");

            empty.getChildren().addAll(ico, msg);
            apuestasContainer.getChildren().add(empty);
            return;
        }

        String textoContador = activas.size() == 1
                ? "1 apuesta en curso"
                : "" + activas.size() + " apuestas en curso";
        Label contador = new Label(textoContador);
        contador.setStyle("-fx-text-fill: #ECEFF1; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0 0 6 0;");
        apuestasContainer.getChildren().add(contador);

        for (Apuesta a : activas) {
            apuestasContainer.getChildren().add(crearTarjetaApuesta(a));
        }
    }

    private VBox crearTarjetaApuesta(Apuesta apuesta) {
        Caballo caballo = caballoDAO.findById(apuesta.getIdCaballo());
        String infoCaballo = caballo != null
                ? caballo.getNombre() + "  —  No. " + caballo.getNumero()
                : "Caballo #" + apuesta.getIdCaballo();

        double porcentaje = 100.0 / apuesta.getMultiplicador();
        double premioEsperad = apuesta.getMonto() * apuesta.getMultiplicador();

        Label lblCaballo = new Label(infoCaballo);
        lblCaballo.setStyle("-fx-text-fill: #ECEFF1; -fx-font-weight: bold; -fx-font-size: 13px;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #3D4454;");

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(6);
        grid.setStyle("-fx-padding: 4 0 0 0;");

        grid.add(crearLabel(String.format("Probabilidad: %.1f%%", porcentaje)),0,0);
        grid.add(crearLabel(String.format("Apuesta: $%.2f", apuesta.getMonto())),1,0);
        grid.add(crearLabel("Carrera #" + apuesta.getIdCarrera()),0,1);
        grid.add(crearLabel(String.format("Premio esperado: $%.2f", premioEsperad)),1,1);

        Label lblEstado = new Label("PENDIENTE");
        lblEstado.setStyle("-fx-text-fill: #F39C12; -fx-font-size: 11px; -fx-font-weight: bold;");
        grid.add(lblEstado, 0, 2, 2, 1);

        VBox card = new VBox(8, lblCaballo, sep, grid);
        card.setStyle("-fx-background-color: #1E222B;" +
                "-fx-border-color: #3D4454;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 12 16 12 16;");
        return card;
    }

    private Label crearLabel(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-text-fill: #BDC3C7; -fx-font-size: 12px;");
        return lbl;
    }

    @FXML
    private void handleVolver() {
        SceneManager.cambiarEscena("dashboard-usuario.fxml");
    }
}
