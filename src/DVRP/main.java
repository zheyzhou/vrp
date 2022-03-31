package DVRP;

import java.sql.*;
import java.util.ArrayList;

public class main {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/vrp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    static final String user = "root";
    static final String password = "wsxzzwals";

    static private final Sol sol = new Sol();
    static private final ArrayList<customer> tmp_cust_seq = sol.getCust_seq();
    static private final calcost calcost = new calcost();

    //读入顾客点
    public static void setcust(){
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);

            conn = DriverManager.getConnection(DB_URL, user, password);

            stmt = conn.createStatement();

            String sql;
            sql = "SELECT id,x坐标,y坐标,demand,earliest,latest,sever FROM customer";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                customer tmp_cust = new customer();
                tmp_cust.setId(rs.getInt("id"));
                tmp_cust.setX(rs.getDouble("x坐标"));
                tmp_cust.setY(rs.getDouble("y坐标"));
                tmp_cust.setDemand(rs.getDouble("demand"));
                tmp_cust.setT1(rs.getTime("earliest"));
                tmp_cust.setT2(rs.getTime("latest"));
                tmp_cust.setSever(rs.getDouble("sever"));
                tmp_cust_seq.add(tmp_cust);
                sol.setCust_seq(tmp_cust_seq);
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    //赋值
    public static void new_sol(Sol s1,Sol s2){
        for (customer c : s2.getCust_seq()){
            s1.getCust_seq().add(new customer(c.getId(),c.getX(),c.getY(),
                    c.getDemand(),c.getE(),c.getL(),c.getSever()));
        }
        s1.setFit(s2.getFit());
        s1.setCost(s2.getCost());
    }

    //把路径独立出来
    private static void depend(ArrayList<Sol> ss, Sol s){
        Sol so = new Sol();
        so.getCust_seq().add(s.getCust_seq().get(0));
        for (int i=1;i<s.getCust_seq().size();i++){
            if (s.getCust_seq().get(i).getId() == 0){
                so.getCust_seq().add(s.getCust_seq().get(i));
                ss.add(so);
                so = new Sol();
            }
            so.getCust_seq().add(s.getCust_seq().get(i));
        }
        so.getCust_seq().add(s.getCust_seq().get(0));
        ss.add(so);
    }

    //输出函数
    private static void print(ArrayList<Sol> ss){
        double sum_cost = 0;
        for (Sol s : ss){
            System.out.println("-------");
            sum_cost += calcost.cal_tarval(s);
            System.out.println("第"+(ss.indexOf(s)+1)+"条路径为：");
            for (customer c : s.getCust_seq()){
                System.out.print(c.getId()+" ");
            }
            System.out.println();
            System.out.println("行驶成本："+calcost.cal_driving_cost(s));
            System.out.println("时间成本："+calcost.cal_ela_cost(s));
            System.out.println("固定成本："+calcost.cal_fixed_cost());
            System.out.println("总成本："+calcost.cal_tarval(s));
            System.out.println("早到总时长："+s.getEarly_time());
            System.out.println("迟到总时长："+s.getLate_time());
        }

        System.out.println("所有路径总成本："+sum_cost);
        System.out.println("---------------------------------------------------");
    }

    public static void main(String[] args) {
        //读入顾客点
        setcust();

        //用遗传算法得到初始路径
        GA ga = new GA();
        ga.start(sol);

        //得到初始路径
        Sol tmp = ga.getBest_sol();
        Sol first_sol = new Sol();
        new_sol(first_sol, tmp);

        //输出初始路径，首先将各路径独立，再输出
        ArrayList<Sol> first_ss = new ArrayList<>();
        depend(first_ss, first_sol);
        System.out.println("静态顾客点配送最优路径：");
        print(first_ss);
        print print = new print(first_sol);


        //实时更新路径
        update update = new update();
        update.auto_update(first_sol);

        //得到最终结果
        Sol tmp2 = update.getBest_sol();
        Sol last_sol = new Sol();
        new_sol(last_sol, tmp2);

        //输出最终路径，首先将各路径独立，再输出
        ArrayList<Sol> last_ss = new ArrayList<>();
        depend(last_ss, last_sol);
        System.out.println("最终顾客点配送最优路径：");
        print(last_ss);
        print print1 = new print(last_sol);

        //算法结束
        System.out.println("当日配送已完成！");
    }
}
