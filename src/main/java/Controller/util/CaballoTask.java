package Controller.util;

import javafx.concurrent.Task;
import java.util.Random;

public class CaballoTask extends Task<Double> {

    private final int duracionSeg;
    private volatile double progresoActual = 0.0;
    private final Random random = new Random();

    // duracionSeg: duración total de la carrera en segundos (máx 25)
    // Se usa para calibrar la velocidad de avance por tick
    public CaballoTask(int duracionSeg) {
        this.duracionSeg = duracionSeg;
    }

    @Override
    protected Double call() throws InterruptedException {

        // Fórmula de calibración:
        // - Un tick ocurre cada 100ms → duracionSeg * 10 ticks en total
        // - El avance por tick es aleatorio entre [0, maxAvancePorTick]
        // - Con maxAvancePorTick = 170 / (duracionSeg * 10), el promedio
        //   al final es ~85%, con variabilidad natural:
        //   algunos caballos terminan (llegan al 100%), otros no
        double maxAvancePorTick = 170.0 / (duracionSeg * 10.0);

        while (progresoActual < 100.0 && !isCancelled()) {
            Thread.sleep(100);
            double avance = random.nextDouble() * maxAvancePorTick;
            progresoActual = Math.min(100.0, progresoActual + avance);

            // updateProgress actualiza progressProperty() de 0.0 a 1.0
            // El ProgressBar en la vista se enlaza a task.progressProperty()
            updateProgress(progresoActual, 100.0);
        }

        return progresoActual; // valor 0–100 que se guarda en BD como progreso_final
    }

    // El SimulacionService llama este método DESPUÉS de que el task termina
    // (ya sea por cancel() o por llegar al 100%)
    public double getProgresoActual() {
        return progresoActual;
    }

    // true si llegó al 100% — distingue "terminó" de "se quedó a medias"
    public boolean terminoCarrera() {
        return progresoActual >= 100.0;
    }
}