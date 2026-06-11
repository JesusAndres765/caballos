package Model.dao;

import Model.Transaccion;
import Model.enums.TipoTransaccion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransaccionDAO {

    private final Connection conn = ConexionDB.getConnection();

    public boolean insert(Transaccion transaccion) {
        String sql = "INSERT INTO transacciones (id_usuario, tipo, monto, descripcion) " + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, transaccion.getIdUsuario());
            ps.setString(2, transaccion.getTipo().name());
            ps.setDouble(3, transaccion.getMonto());
            ps.setString(4, transaccion.getDescripcion());
            if (ps.executeUpdate() > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) transaccion.setIdTransaccion(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("TransaccionDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public List<Transaccion> findByUsuario(int idUsuario) {
        List<Transaccion> lista = new ArrayList<>();
        String sql = "SELECT * FROM transacciones WHERE id_usuario = ? ORDER BY fecha DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("TransaccionDAO.findByUsuario: " + e.getMessage());
        }
        return lista;
    }

    private Transaccion mapResultSet(ResultSet rs) throws SQLException {
        Transaccion t = new Transaccion();
        t.setIdTransaccion(rs.getInt("id_transaccion"));
        t.setIdUsuario(rs.getInt("id_usuario"));
        t.setTipo(TipoTransaccion.valueOf(rs.getString("tipo")));
        t.setMonto(rs.getDouble("monto"));
        t.setDescripcion(rs.getString("descripcion"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) t.setFecha(ts.toLocalDateTime());
        return t;
    }
}