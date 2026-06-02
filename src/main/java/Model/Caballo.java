package Model;

public class Caballo {

    private int idCaballo;
    private String nombre;
    private int numero;
    private int carrerasCorridas;
    private int carrerasGanadas;

    public Caballo() {}

    public Caballo(int idCaballo, String nombre, int numero,
                   int carrerasCorridas, int carrerasGanadas) {
        this.idCaballo = idCaballo;
        this.nombre = nombre;
        this.numero = numero;
        this.carrerasCorridas = carrerasCorridas;
        this.carrerasGanadas = carrerasGanadas;
    }

    public int getIdCaballo() { return idCaballo; }
    public void setIdCaballo(int idCaballo) { this.idCaballo = idCaballo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public int getCarrerasCorridas() { return carrerasCorridas; }
    public void setCarrerasCorridas(int carrerasCorridas) { this.carrerasCorridas = carrerasCorridas; }

    public int getCarrerasGanadas() { return carrerasGanadas; }
    public void setCarrerasGanadas(int carrerasGanadas) { this.carrerasGanadas = carrerasGanadas; }

    @Override
    public String toString() {
        return "Caballo{id=" + idCaballo + ", nombre='" + nombre + "', numero=" + numero + "}";
    }
}