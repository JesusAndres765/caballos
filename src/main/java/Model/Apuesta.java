package Model;

import Model.enums.ResultadoApuesta;
import java.time.LocalDateTime;

public class Apuesta {
    private int idApuesta;
    private int idUsuario;
    private int idCarrera;
    private int idCaballo;
    private double monto;
    private int multiplicador;
    private ResultadoApuesta resultado;
    private double cobro;
    private LocalDateTime fechaApuesta;

    public Apuesta() {}

    public Apuesta(int idApuesta, int idUsuario, int idCarrera, int idCaballo, double monto, int multiplicador, ResultadoApuesta resultado, double cobro, LocalDateTime fechaApuesta) {
        this.idApuesta = idApuesta;
        this.idUsuario = idUsuario;
        this.idCarrera = idCarrera;
        this.idCaballo = idCaballo;
        this.monto = monto;
        this.multiplicador = multiplicador;
        this.resultado = resultado;
        this.cobro = cobro;
        this.fechaApuesta = fechaApuesta;
    }

    public int getIdApuesta() { return idApuesta; }
    public void setIdApuesta(int idApuesta) { this.idApuesta = idApuesta; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public int getIdCarrera() { return idCarrera; }
    public void setIdCarrera(int idCarrera) { this.idCarrera = idCarrera; }

    public int getIdCaballo() { return idCaballo; }
    public void setIdCaballo(int idCaballo) { this.idCaballo = idCaballo; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public int getMultiplicador() { return multiplicador; }
    public void setMultiplicador(int multiplicador) { this.multiplicador = multiplicador; }

    public ResultadoApuesta getResultado() { return resultado; }
    public void setResultado(ResultadoApuesta resultado) { this.resultado = resultado; }

    public double getCobro() { return cobro; }
    public void setCobro(double cobro) { this.cobro = cobro; }

    public LocalDateTime getFechaApuesta() { return fechaApuesta; }
    public void setFechaApuesta(LocalDateTime fechaApuesta) { this.fechaApuesta = fechaApuesta; }

    @Override
    public String toString() {
        return "Apuesta{id=" + idApuesta + ", usuario=" + idUsuario + ", caballo=" + idCaballo + ", monto=" + monto + ", resultado=" + resultado + ", cobro=" + cobro + "}";
    }
}