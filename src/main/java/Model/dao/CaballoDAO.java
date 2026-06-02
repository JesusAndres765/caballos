package Model.dao;

import Model.Caballo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CaballoDAO {

    private final Connection conn = ConexionDB.getConnection();

    // Registra un nuevo caballo
    public boolean insert(Caballo caballo) {
        String sql = "INSERT INTO caballos (nombre, numero) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, caballo.getNombre());
            ps.setInt(2, caballo.getNumero());
            if (ps.executeUpdate() > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) caballo.setIdCaballo(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("CaballoDAO.insert: " + e.getMessage());
        }
        return false;
    }

    // Devuelve todos los caballos — para el dropdown de Crear Carrera
    public List<Caballo> findAll() {
        List<Caballo> lista = new ArrayList<>();
        String sql = "SELECT * FROM caballos ORDER BY numero";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("CaballoDAO.findAll: " + e.getMessage());
        }
        return lista;
    }

    // Busca por ID — usado al construir el dashboard de una carrera
    public Caballo findById(int idCaballo) {
        String sql = "SELECT * FROM caballos WHERE id_caballo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCaballo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("CaballoDAO.findById: " + e.getMessage());
        }
        return null;
    }

    // Verifica que el número identificador sea único antes de insertar
    public boolean existeNumero(int numero) {
        String sql = "SELECT COUNT(*) FROM caballos WHERE numero = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numero);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("CaballoDAO.existeNumero: " + e.getMessage());
        }
        return false;
    }

    // Incrementa contadores al cerrar una carrera
    // gano=true suma carreras_corridas + carreras_ganadas
    // gano=false solo suma carreras_corridas
    public boolean actualizarContadores(int idCaballo, boolean gano) {
        String sql = gano
                ? "UPDATE caballos SET carreras_corridas = carreras_corridas + 1, carreras_ganadas = carreras_ganadas + 1 WHERE id_caballo = ?"
                : "UPDATE caballos SET carreras_corridas = carreras_corridas + 1 WHERE id_caballo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCaballo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("CaballoDAO.actualizarContadores: " + e.getMessage());
        }
        return false;
    }

    private Caballo mapResultSet(ResultSet rs) throws SQLException {
        Caballo c = new Caballo();
        c.setIdCaballo(rs.getInt("id_caballo"));
        c.setNombre(rs.getString("nombre"));
        c.setNumero(rs.getInt("numero"));
        c.setCarrerasCorridas(rs.getInt("carreras_corridas"));
        c.setCarrerasGanadas(rs.getInt("carreras_ganadas"));
        return c;
    }
}