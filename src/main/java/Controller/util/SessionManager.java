package Controller.util;

import Model.Usuario;
import Model.enums.Rol;

public class SessionManager {

    private static SessionManager instance = null;
    private Usuario usuarioActual = null;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Guarda el usuario al hacer login
    public void iniciarSesion(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    // Limpia la sesión al hacer logout
    public void cerrarSesion() {
        this.usuarioActual = null;
    }

    // Devuelve el usuario logueado — usado en todos los controladores
    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public boolean isLogueado() {
        return usuarioActual != null;
    }

    public boolean isAdmin() {
        return usuarioActual != null && usuarioActual.getRol() == Rol.ADMIN;
    }

    // Actualiza el saldo en memoria tras depósito, retiro o cobro de apuesta
    // Evita hacer un SELECT a BD solo para refrescar un número
    public void refrescarSaldo(double nuevoSaldo) {
        if (usuarioActual != null) {
            usuarioActual.setSaldo(nuevoSaldo);
        }
    }
}