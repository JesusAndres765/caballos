package Model.dao;

import Model.CarreraCaballo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarreraCaballoDAO {

    private final Connection conn = ConexionDB.getConnection();

    public boolean insert(CarreraCaballo cc) {
        String sql = "INSERT INTO carrera_caballos (id_carrera, id_caballo) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cc.getIdCarrera());
            ps.setInt(2, cc.getIdCaballo());
            if (ps.executeUpdate() > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) cc.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("CarreraCaballoDAO.insert: " + e.getMessage());
        }
        return false;
    }

    public List<CarreraCaballo> findByCarrera(int idCarrera) {
        List<CarreraCaballo> lista = new ArrayList<>();
        String sql = "SELECT * FROM carrera_caballos WHERE id_carrera = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCarrera);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("CarreraCaballoDAO.findByCarrera: " + e.getMessage());
        }
        return lista;
    }

    public boolean updateResultado(int id, int posicion, double progreso, boolean termino) {
        String sql = "UPDATE carrera_caballos " + "SET posicion_final = ?, progreso_final = ?, termino_carrera = ? " + "WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, posicion);
            ps.setDouble(2, progreso);
            ps.setBoolean(3, termino);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("CarreraCaballoDAO.updateResultado: " + e.getMessage());
        }
        return false;
    }

    private CarreraCaballo mapResultSet(ResultSet rs) throws SQLException {
        CarreraCaballo cc = new CarreraCaballo();
        cc.setId(rs.getInt("id"));
        cc.setIdCarrera(rs.getInt("id_carrera"));
        cc.setIdCaballo(rs.getInt("id_caballo"));
        cc.setPosicionFinal(rs.getInt("posicion_final"));
        cc.setProgresoFinal(rs.getDouble("progreso_final"));
        cc.setTerminoCarrera(rs.getBoolean("termino_carrera"));
        return cc;
    }
}