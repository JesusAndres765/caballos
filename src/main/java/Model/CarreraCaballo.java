package Model;

public class CarreraCaballo {
    private int id;
    private int idCarrera;
    private int idCaballo;
    private int posicionFinal;
    private double progresoFinal;
    private boolean terminoCarrera;

    public CarreraCaballo() {}

    public CarreraCaballo(int id, int idCarrera, int idCaballo,
                          int posicionFinal, double progresoFinal, boolean terminoCarrera) {
        this.id = id;
        this.idCarrera = idCarrera;
        this.idCaballo = idCaballo;
        this.posicionFinal = posicionFinal;
        this.progresoFinal = progresoFinal;
        this.terminoCarrera = terminoCarrera;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdCarrera() { return idCarrera; }
    public void setIdCarrera(int idCarrera) { this.idCarrera = idCarrera; }

    public int getIdCaballo() { return idCaballo; }
    public void setIdCaballo(int idCaballo) { this.idCaballo = idCaballo; }

    public int getPosicionFinal() { return posicionFinal; }
    public void setPosicionFinal(int posicionFinal) { this.posicionFinal = posicionFinal; }

    public double getProgresoFinal() { return progresoFinal; }
    public void setProgresoFinal(double progresoFinal) { this.progresoFinal = progresoFinal; }

    public boolean isTerminoCarrera() { return terminoCarrera; }
    public void setTerminoCarrera(boolean terminoCarrera) { this.terminoCarrera = terminoCarrera; }

    @Override
    public String toString() {
        return "CarreraCaballo{carrera=" + idCarrera + ", caballo=" + idCaballo
                + ", posicion=" + posicionFinal + ", progreso=" + progresoFinal
                + ", termino=" + terminoCarrera + "}";
    }
}