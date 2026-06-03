package Model.dao;

import Model.Caballo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CaballoDAO {

    private final Connection conn = ConexionDB.getConnection();

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


    // Actualiza nombre y número de un caballo existente
    public boolean update(Caballo caballo) {
        String sql = "UPDATE caballos SET nombre = ?, numero = ? WHERE id_caballo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, caballo.getNombre());
            ps.setInt(2, caballo.getNumero());
            ps.setInt(3, caballo.getIdCaballo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("CaballoDAO.update: " + e.getMessage());
        }
        return false;
    }

    // Elimina el caballo junto con todos sus registros dependientes.
// Orden obligatorio por FK: apuestas → carrera_caballos → caballos.
// Todo dentro de una transacción: si un paso falla, se hace rollback.
    public boolean deleteConCascada(int idCaballo) {
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM apuestas WHERE id_caballo = ?")) {
                ps.setInt(1, idCaballo);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM carrera_caballos WHERE id_caballo = ?")) {
                ps.setInt(1, idCaballo);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM caballos WHERE id_caballo = ?")) {
                ps.setInt(1, idCaballo);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {
                System.err.println("CaballoDAO.deleteConCascada rollback: " + ex.getMessage());
            }
            System.err.println("CaballoDAO.deleteConCascada: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) {
                System.err.println("CaballoDAO.deleteConCascada autoCommit: " + e.getMessage());
            }
        }
        return false;
    }

    // Devuelve true solo si el caballo está en una carrera que AÚN NO terminó.
// Carreras FINALIZADAS no bloquean la eliminación.
    public boolean estaEnCarreraActiva(int idCaballo) {
        String sql = "SELECT COUNT(*) " +
                "FROM carrera_caballos cc " +
                "INNER JOIN carreras c ON cc.id_carrera = c.id_carrera " +
                "WHERE cc.id_caballo = ? " +
                "  AND c.estado IN ('EN_GATERA', 'EN_CURSO')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCaballo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("CaballoDAO.estaEnCarreraActiva: " + e.getMessage());
        }
        return false;
    }

    // Busca por ID exacto, por nombre parcial, o por ambos (OR)
// Si ambos campos están vacíos devuelve todos
    public List<Caballo> buscar(String nombre, String idTexto) {
        boolean tieneId     = idTexto != null && !idTexto.trim().isEmpty();
        boolean tieneNombre = nombre  != null && !nombre.trim().isEmpty();

        if (!tieneId && !tieneNombre) return findAll();

        String sql;
        if (tieneId && tieneNombre) {
            sql = "SELECT * FROM caballos WHERE id_caballo = ? OR nombre LIKE ? ORDER BY id_caballo";
        } else if (tieneId) {
            sql = "SELECT * FROM caballos WHERE id_caballo = ? ORDER BY id_caballo";
        } else {
            sql = "SELECT * FROM caballos WHERE nombre LIKE ? ORDER BY id_caballo";
        }

        List<Caballo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (tieneId && tieneNombre) {
                ps.setInt(1, Integer.parseInt(idTexto.trim()));
                ps.setString(2, "%" + nombre.trim() + "%");
            } else if (tieneId) {
                ps.setInt(1, Integer.parseInt(idTexto.trim()));
            } else {
                ps.setString(1, "%" + nombre.trim() + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapResultSet(rs));
        } catch (SQLException | NumberFormatException e) {
            System.err.println("CaballoDAO.buscar: " + e.getMessage());
        }
        return lista;
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