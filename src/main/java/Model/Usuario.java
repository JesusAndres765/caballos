package Model;

import Model.enums.Rol;

import java.time.LocalDateTime;

public class Usuario {

    private int idUsuario;
    private String username;
    private String contrasena;
    private Rol rol;
    private double saldo;
    private LocalDateTime fechaRegistro;

    public Usuario() {}

    public Usuario(int idUsuario, String username, String contrasena,
                   Rol rol, double saldo, LocalDateTime fechaRegistro) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.contrasena = contrasena;
        this.rol = rol;
        this.saldo = saldo;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    @Override
    public String toString() {
        return "Usuario{id=" + idUsuario + ", username='" + username + "', rol=" + rol + ", saldo=" + saldo + "}";
    }
}