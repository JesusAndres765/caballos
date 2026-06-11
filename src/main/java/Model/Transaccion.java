package Model;

import Model.enums.TipoTransaccion;
import java.time.LocalDateTime;

public class Transaccion {
    private int idTransaccion;
    private int idUsuario;
    private TipoTransaccion tipo;
    private double monto;
    private String descripcion;
    private LocalDateTime fecha;

    public Transaccion() {}

    public Transaccion(int idTransaccion, int idUsuario, TipoTransaccion tipo, double monto, String descripcion, LocalDateTime fecha) {
        this.idTransaccion = idTransaccion;
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    public int getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(int idTransaccion) { this.idTransaccion = idTransaccion; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public TipoTransaccion getTipo() { return tipo; }
    public void setTipo(TipoTransaccion tipo) { this.tipo = tipo; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    @Override
    public String toString() {
        return "Transaccion{id=" + idTransaccion + ", usuario=" + idUsuario + ", tipo=" + tipo + ", monto=" + monto + "}";
    }
}