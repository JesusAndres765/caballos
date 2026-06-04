package Controller.util;

import Model.Usuario;
import Model.enums.Rol;

public class SessionManager {

    private static SessionManager instance = null;
    private Usuario usuarioActual = null;

    private SessionManager() {}

    public static SessionManager getInstancia() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void iniciarSesion(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    public void cerrarSesion() {
        this.usuarioActual = null;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public boolean isLogueado() {
        return usuarioActual != null;
    }

    public boolean isAdmin() {
        return usuarioActual != null && usuarioActual.getRol() == Rol.ADMIN;
    }

    public void refrescarSaldo(double nuevoSaldo) {
        if (usuarioActual != null) {
            usuarioActual.setSaldo(nuevoSaldo);
        }
    }
}