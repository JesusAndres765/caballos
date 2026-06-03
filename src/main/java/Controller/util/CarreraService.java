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

/**
 * Servicio singleton que corre cada segundo en el hilo de JavaFX.
 * Es el único responsable de:
 *  - Transicionar carreras de EN_GATERA → EN_CURSO cuando llega la hora
 *  - Transicionar de EN_CURSO → FINALIZADA cuando se acaba el tiempo
 *  - Calcular resultados y liquidar apuestas
 *
 * VerCarreraController es solo un visor que hace polling a este servicio.
 */
public class CarreraService {

    private static CarreraService instancia;
    private Timeline timeline;

    // Cada instancia de DAO usa la conexión compartida — al correr en el hilo
    // de JavaFX no hay concurrencia, así que no hay riesgo de colisión
    private final CarreraDAO        carreraDAO        = new CarreraDAO();
    private final CarreraCaballoDAO carreraCaballoDAO = new CarreraCaballoDAO();
    private final CaballoDAO        caballoDAO        = new CaballoDAO();
    private final ApuestaDAO        apuestaDAO        = new ApuestaDAO();
    private final UsuarioDAO        usuarioDAO        = new UsuarioDAO();
    private final TransaccionDAO    transaccionDAO    = new TransaccionDAO();

    private CarreraService() {}

    public static CarreraService getInstance() {
        if (instancia == null) instancia = new CarreraService();
        return instancia;
    }

    /** Llamar desde MainApp.start() — después de que JavaFX esté listo */
    public void iniciar() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void detener() {
        if (timeline != null) timeline.stop();
    }

    // ── Núcleo del servicio ───────────────────────────────────────────────────

    private void tick() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            for (Carrera c : carreraDAO.findActivas()) {
                procesarCarrera(c, ahora);
            }
        } catch (Exception ex) {
            System.err.println("CarreraService.tick: " + ex.getMessage());
        }
    }

    private void procesarCarrera(Carrera c, LocalDateTime ahora) {
        switch (c.getEstado()) {

            case EN_GATERA:
                LocalDateTime inicio = c.getFechaInicio();
                if (inicio != null && !ahora.isBefore(inicio)) {
                    // Llegó la hora — arranca la carrera
                    carreraDAO.updateEstado(c.getIdCarrera(), EstadoCarrera.EN_CURSO);
                    carreraDAO.updateFechaInicio(c.getIdCarrera(), ahora);
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

    // ── Finalización ─────────────────────────────────────────────────────────

    private void finalizarCarrera(Carrera carrera) {
        List<CarreraCaballo> inscripciones =
                carreraCaballoDAO.findByCarrera(carrera.getIdCarrera());

        if (inscripciones.isEmpty()) {
            carreraDAO.updateEstado(carrera.getIdCarrera(), EstadoCarrera.FINALIZADA);
            return;
        }

        // Simula el progreso de cada caballo con el mismo algoritmo de CaballoTask
        Random rng = new Random();
        double maxAvance  = 170.0 / (carrera.getDuracionSeg() * 10.0);
        int    totalTicks = carrera.getDuracionSeg() * 10;

        List<double[]> resultados = new ArrayList<>(); // {índice, progreso 0-100}
        for (int i = 0; i < inscripciones.size(); i++) {
            double prog = 0;
            for (int t = 0; t < totalTicks; t++) {
                prog = Math.min(100.0, prog + rng.nextDouble() * maxAvance);
            }
            resultados.add(new double[]{ i, prog });
        }
        resultados.sort((a, b) -> Double.compare(b[1], a[1])); // mayor primero = 1er lugar

        int idCaballoGanador = -1;
        for (int pos = 0; pos < resultados.size(); pos++) {
            int    idx     = (int) resultados.get(pos)[0];
            double prog    = resultados.get(pos)[1];
            CarreraCaballo cc = inscripciones.get(idx);
            boolean termino   = prog >= 100.0;

            carreraCaballoDAO.updateResultado(cc.getId(), pos + 1, prog / 100.0, termino);
            caballoDAO.actualizarContadores(cc.getIdCaballo(), pos == 0);

            if (pos == 0) idCaballoGanador = cc.getIdCaballo();
        }

        carreraDAO.updateEstado(carrera.getIdCarrera(), EstadoCarrera.FINALIZADA);
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