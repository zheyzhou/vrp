package DVRP;

import java.sql.*;
import java.util.ArrayList;

public class update {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/vrp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    static final String user = "root";
    static final String password = "wsxzzwals";

    private final getspeed getspeed = new getspeed();//不同时段的车速
    private final ArrayList<customer> new_cus = new ArrayList<>();//新增的客户点
    private Sol best_sol = new Sol();//最优解

    public void auto_update(Sol sol){
        //把路径独立出来
        ArrayList<Sol> ss = new ArrayList<>();
        depend(ss, sol);

        //将已配送的顾客点整合为一个虚拟顾客点后的路径
        ArrayList<Sol> ns = new ArrayList<>();
        //已配送的顾客点
        ArrayList<Sol> os = new ArrayList<>();

        //配送中心开始配送时间t0，结束配送时间t1,更新周期td
        double t0 = 10,t1 = 600,td = 30,t = t0;
        double v = getspeed.get_speed(t);

        while (t < t1){
            //检查数据库中是否有新的客户点,若有,则将其取出
            if (judge_new(t)){
                for (Sol item : ss){
                    int whole = 1;//是否全部完成，0部分完成，1全部完成
                    double tt = t0, ttt = tt;
                    for (int i=1;i<item.getCust_seq().size();i++){
                        //计算车辆达到下一个顾客点的时间
                        customer c1 = item.getCust_seq().get(i-1);
                        customer c2 = item.getCust_seq().get(i);
                        caldistance caldistance = new caldistance(c1.getX(),c1.getY(),c2.getX(),c2.getY());
                        double dis = caldistance.distance();
                        tt += dis/v*60;
                        if (getspeed.get_speed(tt) != v) {
                            double vv = getspeed.get_speed(tt);
                            double tm = getspeed.get_minutes(vv);
                            tt -= dis / v * 60;
                            tt += tm - tt + (dis - (tm - tt) / 60 * v) / vv * 60;
                            v = vv;
                        }

                        //若到达下个顾客点时到达更新时刻,则构建新的路径
                        if (tt >= t){
                            double demand = 0;

                            //存储已服务的顾客点
                            Sol tmp_s = new Sol();
                            for (int j=0;j<i;j++){
                                customer tmp_c = item.getCust_seq().get(j);
                                demand += tmp_c.getDemand();
                                tmp_s.getCust_seq().add(new customer(tmp_c.getId(),tmp_c.getX(),tmp_c.getY(),
                                        tmp_c.getDemand(),tmp_c.getE(),tmp_c.getL(),tmp_c.getSever()));
                            }
                            os.add(tmp_s);

                            //存储未服务的顾客点
                            tmp_s = new Sol();
                            //虚拟顾客点
                            tmp_s.getCust_seq().add(new customer(sol.getCust_seq().size()+100,
                                    item.getCust_seq().get(i-1).getX(),item.getCust_seq().get(i-1).getY(),
                                    demand,ttt,0,0));
                            for (int j=i;j<item.getCust_seq().size();j++){
                                customer tmp_c = item.getCust_seq().get(j);
                                tmp_s.getCust_seq().add(new customer(tmp_c.getId(),tmp_c.getX(),tmp_c.getY(),
                                        tmp_c.getDemand(),tmp_c.getE(),tmp_c.getL(),tmp_c.getSever()));
                            }
                            ns.add(tmp_s);

                            whole = 0;//部分完成
                            break;
                        }

                        //加上服务时间
                        tt += item.getCust_seq().get(i).getSever();
                        ttt = tt;
                    }
                    //如果全部完成
                    if (whole == 1){
                        double demand = 0;

                        //存储已服务的顾客点
                        Sol tmp_s = new Sol();
                        for (customer c : item.getCust_seq()){
                            demand += c.getDemand();
                            tmp_s.getCust_seq().add(new customer(c.getId(),c.getX(),c.getY(),
                                    c.getDemand(),c.getE(),c.getL(),c.getSever()));
                        }
                        os.add(tmp_s);

                        //存储未服务的顾客点
                        tmp_s = new Sol();
                        //虚拟顾客点
                        tmp_s.getCust_seq().add(new customer(sol.getCust_seq().size()+100,
                                item.getCust_seq().get(item.getCust_seq().size()-1).getX(),
                                item.getCust_seq().get(item.getCust_seq().size()-1).getY(),
                                demand,ttt,0,0));
                        ns.add(tmp_s);
                    }
                }

                //使用模拟退火算法更新路径
                Sol los = update_pop(ns,os);
                sol = new Sol();
                for (customer c : los.getCust_seq()){
                    sol.getCust_seq().add(c);
                }

                //输出相关信息
                print(ns,os,t,sol);

                sol.setCost(los.getCost());
                ss.clear();
                ns.clear();
                os.clear();
                depend(ss, sol);
            }
            t += td;
        }

        best_sol = sol;
    }

    //使用模拟退火算法更新路径
    private Sol update_pop(ArrayList<Sol> ns, ArrayList<Sol> os){
        SA sa = new SA();
        sa.start(ns,os,new_cus);
        return sa.getBest_sol();
    }

    //把路径独立出来
    private void depend(ArrayList<Sol> ss, Sol s){
        Sol so = new Sol();
        so.getCust_seq().add(s.getCust_seq().get(0));
        for (int i=1;i<s.getCust_seq().size();i++){
            if (s.getCust_seq().get(i).getId() == 0){
                ss.add(so);
                so = new Sol();
            }
            so.getCust_seq().add(s.getCust_seq().get(i));
        }
        ss.add(so);
    }

    //检查数据库中是否有新的客户点,若有,则将其取出
    private boolean judge_new(double t){
        new_cus.clear();
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName(JDBC_DRIVER);

            conn = DriverManager.getConnection(DB_URL, user, password);

            stmt = conn.createStatement();

            String sql;
            sql = "SELECT id,x坐标,y坐标,demand,earliest,latest,sever,cometime FROM dynamic_customer";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Time time = rs.getTime("cometime");
                int h = (time.getHours()+16)<24 ? time.getHours()+16 : time.getHours()-8;
                time.setHours(h);
                double ti = (time.getHours()-8)*60+time.getMinutes()+time.getSeconds()/60;
                if (ti < t && ti > t - 30){
                    customer tmp_cus = new customer();
                    tmp_cus.setId(rs.getInt("id"));
                    tmp_cus.setX(rs.getDouble("x坐标"));
                    tmp_cus.setY(rs.getDouble("y坐标"));
                    tmp_cus.setDemand(rs.getDouble("demand"));
                    tmp_cus.setT1(rs.getTime("earliest"));
                    tmp_cus.setT2(rs.getTime("latest"));
                    tmp_cus.setSever(rs.getDouble("sever"));
                    new_cus.add(tmp_cus);
                }
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return new_cus.size() != 0;
    }

    //输出函数
    private void print(ArrayList<Sol> ns,ArrayList<Sol> os,double t,Sol sol){
        calcost calcost = new calcost();

        System.out.println("第 "+t+" 分钟更新如下：");

        System.out.print("未完成：");
        for (Sol s1 : ns){
            for (customer c1 : s1.getCust_seq()){
                System.out.print(c1.getId()+" ");
            }
        }
        System.out.println();
        System.out.print("已完成：");
        for (Sol s1 : os){
            for (customer c1 : s1.getCust_seq()){
                System.out.print(c1.getId()+" ");
            }
        }
        System.out.println();

        ArrayList<Sol> ss = new ArrayList<>();
        depend(ss,sol);
        double sum_cost = 0;

        System.out.println("插入新解后：");
        for (Sol s : ss){
            System.out.println("-------");
            s.getCust_seq().add(s.getCust_seq().get(0));
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
        print print2 = new print(sol);
    }

    public Sol getBest_sol() {
        return best_sol;
    }
}
