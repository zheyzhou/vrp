package DVRP;

import java.sql.*;

//车辆及配送相关参数
public class parameter {
    private int Q = 0;//车辆最大载重
    private int c = 0;//单位运距成本
    private int F = 0;//车辆固定成本
    private int α = 0;//等待成本
    private int β = 0;//晚到惩罚成本

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/vrp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    static final String user = "root";
    static final String password = "wsxzzwals";

    public parameter() {
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);

            conn = DriverManager.getConnection(DB_URL, user, password);

            stmt = conn.createStatement();

            String sql;
            sql = "SELECT max_payload,unit_haul_dis_cost,fixed_cost,waiting_cost,penalty_cost FROM parameter";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Q = rs.getInt("max_payload");
                c = rs.getInt("unit_haul_dis_cost");
                F = rs.getInt("fixed_cost");
                α = rs.getInt("waiting_cost");
                β = rs.getInt("penalty_cost");
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public int getQ() {
        return Q;
    }

    public int getC() {
        return c;
    }

    public int getF() {
        return F;
    }

    public int getΑ() {
        return α;
    }

    public int getΒ() {
        return β;
    }
}
