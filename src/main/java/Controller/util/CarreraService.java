package Controller.util;

import Model.*;
import Model.dao.*;
import Model.enums.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CarreraService {
    private static CarreraService instancia;
    private Timeline timeline;

    private final CarreraDAO carreraDAO = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO caballoDAO = new CaballoDAO();
    private final ApuestaDAO apuestaDAO = new ApuestaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TransaccionDAO transaccionDAO = new TransaccionDAO();

    private CarreraService() {}

    public static CarreraService getInstancia() {
        if (instancia == null) instancia = new CarreraService();
        return instancia;
    }

    // iniciar en mainApp
    public void iniciar() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> revisarCarrera()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void detener() {
        if (timeline != null) timeline.stop();
    }

    private void revisarCarrera() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            for (Carrera c : carreraDAO.buscarActivas()) {
                revisarCarrera(c, ahora);
            }
        } catch (Exception ex) {
            System.err.println("CarreraService.revisarCarrera: " + ex.getMessage());
        }
    }

    private void revisarCarrera(Carrera c, LocalDateTime ahora) {
        switch (c.getEstado()) {

            case EN_GATERA:
                LocalDateTime inicio = c.getFechaInicio();
                if (inicio != null && !ahora.isBefore(inicio)) {
                    carreraDAO.actualizarEstado(c.getIdCarrera(), EstadoCarrera.EN_CURSO);
                    carreraDAO.actualizarFechaInicio(c.getIdCarrera(), ahora);
                }
                break;

            case EN_CURSO:
                LocalDateTime fechaInicio = c.getFechaInicio();
                if (fechaInicio != null) {
                    long transcurrido = ChronoUnit.SECONDS.between(fechaInicio, ahora);
                    if (transcurrido >= c.getDuracionSeg()) {
                        finalizarCarrera(c);
                    }
                }
                break;

            default:
                break;
        }
    }

    private void finalizarCarrera(Carrera carrera) {
        List<CarreraCaballo> inscripciones = carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());

        if (inscripciones.isEmpty()) {
            carreraDAO.actualizarEstado(carrera.getIdCarrera(), EstadoCarrera.FINALIZADA);
            return;
        }

        Random rng = new Random();
        double maxAvance = 240.0 / (carrera.getDuracionSeg() * 10.0);
        int totalTicks = carrera.getDuracionSeg() * 10;

        List<double[]> resultados = new ArrayList<>();

        for (int i = 0; i < inscripciones.size(); i++) {
            double progreso   = 0;
            int tickFinish = Integer.MAX_VALUE;

            for (int t = 0; t < totalTicks; t++) {
                progreso = Math.min(100.0, progreso + rng.nextDouble() * maxAvance);
                if (progreso >= 100.0 && tickFinish == Integer.MAX_VALUE) {
                    tickFinish = t;
                }
            }
            resultados.add(new double[]{ i, progreso, tickFinish });
        }

        resultados.sort((a, b) -> {
            boolean aTermino = a[2] < Integer.MAX_VALUE;
            boolean bTermino = b[2] < Integer.MAX_VALUE;
            if (aTermino && bTermino) return Double.compare(a[2], b[2]);
            if (aTermino) return -1;
            if (bTermino) return 1;
            return Double.compare(b[1], a[1]);
        });

        int idCaballoGanador = -1;
        for (int pos = 0; pos < resultados.size(); pos++) {
            int idx = (int) resultados.get(pos)[0];
            double progreso = resultados.get(pos)[1];
            boolean termino = resultados.get(pos)[2] < Integer.MAX_VALUE;
            CarreraCaballo cc = inscripciones.get(idx);

            carreraCaballoDAO.updateResultado(cc.getId(), pos + 1, progreso / 100.0, termino);
            caballoDAO.actualizarContadores(cc.getIdCaballo(), pos == 0);
            if (pos == 0) idCaballoGanador = cc.getIdCaballo();
        }

        carreraDAO.actualizarEstado(carrera.getIdCarrera(), EstadoCarrera.FINALIZADA);
        liquidarApuestas(carrera.getIdCarrera(), idCaballoGanador);
    }

    private void liquidarApuestas(int idCarrera, int idCaballoGanador) {
        for (Apuesta a : apuestaDAO.findByCarrera(idCarrera)) {
            if (a.getIdCaballo() == idCaballoGanador) {
                double cobro = a.getMonto() * a.getMultiplicador();
                apuestaDAO.liquidar(a.getIdApuesta(), ResultadoApuesta.GANADA, cobro);
                Usuario ganador = usuarioDAO.findById(a.getIdUsuario());
                if (ganador != null) {
                    usuarioDAO.updateSaldo(ganador.getIdUsuario(), ganador.getSaldo() + cobro);
                    Transaccion t = new Transaccion();
                    t.setIdUsuario(ganador.getIdUsuario());
                    t.setTipo(TipoTransaccion.COBRO);
                    t.setMonto(cobro);
                    t.setDescripcion("Cobro apuesta carrera #" + idCarrera);
                    transaccionDAO.insert(t);
                }
            } else {
                apuestaDAO.liquidar(a.getIdApuesta(), ResultadoApuesta.PERDIDA, 0.0);
            }
        }
    }
}