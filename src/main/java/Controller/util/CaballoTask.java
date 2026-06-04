package Controller.util;

import javafx.concurrent.Task;
import java.util.Random;

public class CaballoTask extends Task<Double> {

    private final int    duracionSeg;
    private final double progresoInicial; // 0-100, para retomar a mitad de carrera
    private volatile double progresoActual;
    private final Random random = new Random();

    // Constructor normal — empieza desde 0
    public CaballoTask(int duracionSeg) {
        this(duracionSeg, 0.0);
    }

    // Constructor para retomar una carrera ya iniciada.
    // progresoInicial: valor entre 0 y 100 que indica cuánto lleva el caballo.
    public CaballoTask(int duracionSeg, double progresoInicial) {
        this.duracionSeg    = duracionSeg;
        this.progresoInicial = Math.min(progresoInicial, 99.0);
        this.progresoActual  = this.progresoInicial;
    }

    @Override
    protected Double call() throws InterruptedException {
        double maxAvancePorTick = 240.0 / (duracionSeg * 10.0);

        // Publica el progreso inicial de inmediato para que la barra aparezca
        // en la posición correcta desde el primer frame
        updateProgress(progresoActual, 100.0);

        while (progresoActual < 100.0 && !isCancelled()) {
            Thread.sleep(100);
            double avance = random.nextDouble() * maxAvancePorTick;
            progresoActual = Math.min(100.0, progresoActual + avance);
            updateProgress(progresoActual, 100.0);
        }
        return progresoActual;
    }

    public double getProgresoActual() { return progresoActual; }
    public boolean terminoCarrera()   { return progresoActual >= 100.0; }
}