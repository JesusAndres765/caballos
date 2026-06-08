package Model.dao;

import Model.Carrera;
import Model.enums.EstadoCarrera;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CarreraDAO {

    private final Connection conn = ConexionDB.getConnection();

    public boolean insert(Carrera carrera) {
        String sql = "INSERT INTO carreras " +
                "(id_admin, num_caballos, duracion_seg, tiempo_gatera, estado, fecha_inicio) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, carrera.getIdAdmin());
            ps.setInt(2, carrera.getNumCaballos());
            ps.setInt(3, carrera.getDuracionSeg());
            ps.setInt(4, carrera.getTiempoGatera());
            ps.setString(5, carrera.getEstado().name());
            ps.setTimestamp(6, Timestamp.valueOf(carrera.getFechaInicio()));
            if (ps.executeUpdate() > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) carrera.setIdCarrera(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("CarreraDAO.insert: " + e.getMessage());
        }
        return false;
    }

    // Busca por ID
    public Carrera buscarId(int idCarrera) {
        String sql = "SELECT * FROM carreras WHERE id_carrera = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCarrera);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultadosSet(rs);
        } catch (SQLException e) {
            System.err.println("CarreraDAO.findById: " + e.getMessage());
        }
        return null;
    }


    public List<Carrera> buscarActivas() {
        List<Carrera> lista = new ArrayList<>();
        String sql = "SELECT * FROM carreras WHERE estado IN ('EN_GATERA','EN_CURSO') " +
                "ORDER BY fecha_creacion DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapResultadosSet(rs));
        } catch (SQLException e) {
            System.err.println("CarreraDAO.findActivas: " + e.getMessage());
        }
        return lista;
    }

    public boolean actualizarEstado(int idCarrera, EstadoCarrera nuevoEstado) {
        String sql = "UPDATE carreras SET estado = ? WHERE id_carrera = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, idCarrera);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("CarreraDAO.updateEstado: " + e.getMessage());
        }
        return false;
    }

    public boolean actualizarFechaInicio(int idCarrera, LocalDateTime fechaInicio) {
        String sql = "UPDATE carreras SET fecha_inicio = ? WHERE id_carrera = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(fechaInicio));
            ps.setInt(2, idCarrera);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("CarreraDAO.updateFechaInicio: " + e.getMessage());
        }
        return false;
    }


    private Carrera mapResultadosSet(ResultSet rs) throws SQLException {
        Carrera c = new Carrera();
        c.setIdCarrera(rs.getInt("id_carrera"));
        c.setIdAdmin(rs.getInt("id_admin"));
        c.setNumCaballos(rs.getInt("num_caballos"));
        c.setDuracionSeg(rs.getInt("duracion_seg"));
        c.setTiempoGatera(rs.getInt("tiempo_gatera"));
        c.setEstado(EstadoCarrera.valueOf(rs.getString("estado")));
        Timestamp tsC = rs.getTimestamp("fecha_creacion");
        if (tsC != null) c.setFechaCreacion(tsC.toLocalDateTime());
        Timestamp tsI = rs.getTimestamp("fecha_inicio");
        if (tsI != null) c.setFechaInicio(tsI.toLocalDateTime());
        return c;
    }
}