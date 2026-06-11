package Model.dao;

import Model.Usuario;
import Model.enums.Rol;

import java.sql.*;

public class UsuarioDAO {

    private final Connection conn = ConexionDB.getConnection();

    public Usuario findByUsername(String username) {
        String sql = "SELECT * FROM usuarios WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("UsuarioDAO.findByUsername: " + e.getMessage());
        }
        return null;
    }

    // Busca por ID
    public Usuario findById(int idUsuario) {
        String sql = "SELECT * FROM usuarios WHERE id_usuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("UsuarioDAO.findById: " + e.getMessage());
        }
        return null;
    }

    // Registro de nuevo usuario o admin
    public boolean insert(Usuario usuario) {
        String sql = "INSERT INTO usuarios (username, contrasena, rol, saldo) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getContrasena());
            ps.setString(3, usuario.getRol().name());
            ps.setDouble(4, usuario.getSaldo());
            if (ps.executeUpdate() > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) usuario.setIdUsuario(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("UsuarioDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public boolean updateSaldo(int idUsuario, double nuevoSaldo) {
        String sql = "UPDATE usuarios SET saldo = ? WHERE id_usuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nuevoSaldo);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UsuarioDAO.updateSaldo: " + e.getMessage());
        }
        return false;
    }

    public boolean existeUsername(String username) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("UsuarioDAO.existeUsername: " + e.getMessage());
        }
        return false;
    }

    private Usuario mapResultSet(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setUsername(rs.getString("username"));
        u.setContrasena(rs.getString("contrasena"));
        u.setRol(Rol.valueOf(rs.getString("rol")));
        u.setSaldo(rs.getDouble("saldo"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) u.setFechaRegistro(ts.toLocalDateTime());
        return u;
    }
}