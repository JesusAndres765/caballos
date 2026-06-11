package Model.dao;

import Model.Apuesta;
import Model.enums.ResultadoApuesta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApuestaDAO {

    private final Connection conn = ConexionDB.getConnection();

    public boolean insert(Apuesta apuesta) {
        String sql = "INSERT INTO apuestas " +
                "(id_usuario, id_carrera, id_caballo, monto, multiplicador, resultado, cobro) " + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, apuesta.getIdUsuario());
            ps.setInt(2, apuesta.getIdCarrera());
            ps.setInt(3, apuesta.getIdCaballo());
            ps.setDouble(4, apuesta.getMonto());
            ps.setInt(5, apuesta.getMultiplicador());
            ps.setString(6, apuesta.getResultado().name());
            ps.setDouble(7, apuesta.getCobro());
            if (ps.executeUpdate() > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) apuesta.setIdApuesta(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ApuestaDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public List<Apuesta> findByUsuarioYCarrera(int idUsuario, int idCarrera) {
        List<Apuesta> lista = new ArrayList<>();
        String sql = "SELECT * FROM apuestas WHERE id_usuario = ? AND id_carrera = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idCarrera);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("ApuestaDAO.findByUsuarioYCarrera: " + e.getMessage());
        }
        return lista;
    }

    // Historial de las apuestas de un usuario
    public List<Apuesta> findByUsuario(int idUsuario) {
        List<Apuesta> lista = new ArrayList<>();
        String sql = "SELECT * FROM apuestas WHERE id_usuario = ? ORDER BY fecha_apuesta DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("ApuestaDAO.findByUsuario: " + e.getMessage());
        }
        return lista;
    }

    public List<Apuesta> findByCarrera(int idCarrera) {
        List<Apuesta> lista = new ArrayList<>();
        String sql = "SELECT * FROM apuestas WHERE id_carrera = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCarrera);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("ApuestaDAO.findByCarrera: " + e.getMessage());
        }
        return lista;
    }

    public boolean liquidar(int idApuesta, ResultadoApuesta resultado, double cobro) {
        String sql = "UPDATE apuestas SET resultado = ?, cobro = ? WHERE id_apuesta = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resultado.name());
            ps.setDouble(2, cobro);
            ps.setInt(3, idApuesta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ApuestaDAO.liquidar: " + e.getMessage());
        }
        return false;
    }

    private Apuesta mapResultSet(ResultSet rs) throws SQLException {
        Apuesta a = new Apuesta();
        a.setIdApuesta(rs.getInt("id_apuesta"));
        a.setIdUsuario(rs.getInt("id_usuario"));
        a.setIdCarrera(rs.getInt("id_carrera"));
        a.setIdCaballo(rs.getInt("id_caballo"));
        a.setMonto(rs.getDouble("monto"));
        a.setMultiplicador(rs.getInt("multiplicador"));
        a.setResultado(ResultadoApuesta.valueOf(rs.getString("resultado")));
        a.setCobro(rs.getDouble("cobro"));
        Timestamp ts = rs.getTimestamp("fecha_apuesta");
        if (ts != null) a.setFechaApuesta(ts.toLocalDateTime());
        return a;
    }
}