package Model;

import Model.enums.EstadoCarrera;
import java.time.LocalDateTime;

public class Carrera {
    private int idCarrera;
    private int idAdmin;
    private int numCaballos;
    private int duracionSeg;
    private int tiempoGatera;
    private EstadoCarrera estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaInicio;

    public Carrera() {}

    public Carrera(int idCarrera, int idAdmin, int numCaballos, int duracionSeg, int tiempoGatera, EstadoCarrera estado, LocalDateTime fechaCreacion, LocalDateTime fechaInicio) {
        this.idCarrera = idCarrera;
        this.idAdmin = idAdmin;
        this.numCaballos = numCaballos;
        this.duracionSeg = duracionSeg;
        this.tiempoGatera = tiempoGatera;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaInicio = fechaInicio;
    }

    public int getIdCarrera() { return idCarrera; }
    public void setIdCarrera(int idCarrera) { this.idCarrera = idCarrera; }

    public int getIdAdmin() { return idAdmin; }
    public void setIdAdmin(int idAdmin) { this.idAdmin = idAdmin; }

    public int getNumCaballos() { return numCaballos; }
    public void setNumCaballos(int numCaballos) { this.numCaballos = numCaballos; }

    public int getDuracionSeg() { return duracionSeg; }
    public void setDuracionSeg(int duracionSeg) { this.duracionSeg = duracionSeg; }

    public int getTiempoGatera() { return tiempoGatera; }
    public void setTiempoGatera(int tiempoGatera) { this.tiempoGatera = tiempoGatera; }

    public EstadoCarrera getEstado() { return estado; }
    public void setEstado(EstadoCarrera estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    @Override
    public String toString() {
        return "Carrera{id=" + idCarrera + ", numCaballos=" + numCaballos + ", duracion=" + duracionSeg + "s, estado=" + estado + "}";
    }
}