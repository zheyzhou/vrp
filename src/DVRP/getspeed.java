package DVRP;

import java.sql.*;

//不同时段的车速
public class getspeed {
    private Time[] time = new Time[5];
    private final double[] speed = new double[5];
    private final double[] minutes = new double[5];

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/vrp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    static final String user = "root";
    static final String password = "wsxzzwals";

    public getspeed() {
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);

            conn = DriverManager.getConnection(DB_URL, user, password);

            stmt = conn.createStatement();

            String sql;
            sql = "SELECT 开始时间,speed FROM speed";
            ResultSet rs = stmt.executeQuery(sql);

            int i = 0;
            while (rs.next()) {
                Time t = rs.getTime("开始时间");
                int h = (t.getHours()+16)<24 ? t.getHours()+16 : t.getHours()-8;
                t.setHours(h);
                time[i] = t;
                minutes[i] = (t.getHours()-8)*60+t.getMinutes()+t.getSeconds()/60;
                speed[i++] = rs.getDouble("speed");
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public double get_speed(double min){
        if (min >= minutes[0] && min < minutes[1]) {
            return speed[0];
        }
        else if (min >= minutes[1] && min < minutes[2]) {
            return speed[1];
        }
        else if (min >= minutes[2] && min < minutes[3]) {
            return speed[2];
        }
        else if (min >= minutes[3] && min < minutes[4]) {
            return speed[3];
        }
        else {
            return speed[4];
        }
    }

    public double get_minutes(double v){
        if (v == speed[0]){
            return minutes[0];
        }
        else if (v == speed[1]){
            return minutes[1];
        }
        else if (v == speed[2]){
            return minutes[2];
        }
        else if (v == speed[3]){
            return minutes[3];
        }
        else {
            return minutes[4];
        }
    }
}
