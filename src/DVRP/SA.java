package DVRP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class SA {
    private final double T0 = 100;//初始温度
    private final double Te = 0.01;//终止温度
    private final double α = 0.95;//降温系数
    private final int num = 200;//内循环次数
    private int id = 0;//虚拟顾客点的序号
    private final parameter parameter = new parameter();
    private final getspeed getspeed = new getspeed();
    private customer center = new customer();//配送中心
    private final ArrayList<Sol> can_sol = new ArrayList<>();//可插入的路径集合
    private Sol tmp_sol = new Sol();
    private final Sol best_sol = new Sol();

    public void start(ArrayList<Sol> ns, ArrayList<Sol> os, ArrayList<customer> nc){
        //保存虚拟顾客点的序号以及配送中心
        id = ns.get(0).getCust_seq().get(0).getId();
        center = os.get(0).getCust_seq().get(0);

        for (customer c : nc){
            //寻找可插入的路径并插入
            choose_insert(ns,c);

            if (can_sol.size() > 0) {
                //对每一条路径用模拟退火算法，选出最好的一条
                for (Sol cs : can_sol) {
                    if (cs.getCust_seq().size() <= 2){
                        cs.setFit(cal_temp(cs));
                        continue;
                    }

                    //求初始解的温度，及目标值
                    double Z0 = cal_temp(cs);

                    //开始退火
                    double t = T0;
                    while (t >= Te) {
                        int n = num;
                        while (n > 0) {
                            Sol tmp_new_sol;
                            //三种邻域搜索结构
                            Random r = new Random();
                            int p = r.nextInt(3);
                            if (p == 0) {
                                tmp_new_sol = change(cs);
                            } else if (p == 1) {
                                tmp_new_sol = reverse(cs);
                            } else {
                                tmp_new_sol = insert(cs);
                            }

                            //Metropolis准则
                            double Zn = cal_temp(tmp_new_sol);//新解的温度，及目标值
                            double Zd = Zn - Z0;//差值
                            if (Zd < 0) {//接受新解
                                accept_new_sol(tmp_new_sol);
                            }
                            else {//以一定概率接受新解
                                double m = Math.random();
                                if (m < Math.exp(-Zd / t)) {
                                    accept_new_sol(tmp_new_sol);
                                }
                            }
                            n--;
                        }
                        t *= α;
                    }

                    //保留最佳的解
                    cs.getCust_seq().clear();
                    get_new(cs, tmp_sol);
                    cs.setFit(cal_temp(cs));
                }

                //最优解的位置
                int pos = 0;

                //从所有可行路径中选择最好的
                accept_new_sol(can_sol.get(0));
                tmp_sol.setFit(can_sol.get(0).getFit());
                for (Sol cs : can_sol) {
                    if (cs.getFit() <= tmp_sol.getFit()) {
                        accept_new_sol(cs);
                        tmp_sol.setFit(cs.getFit());
                        pos = cs.getPos();
                    }
                }

                //把最好的放回ns中
                ns.remove(pos);
                ns.add(pos,tmp_sol);
            }
        }

        //生成最优解
        produce_best_sol(ns,os);

        //计算最优解目标值
        best_sol.setCost(cal_tarval(best_sol));
    }

    //寻找可插入的路径并插入
    private void choose_insert(ArrayList<Sol> ns, customer c){
        can_sol.clear();

        //判断是否未插入
        boolean flag = true;

        //寻找所有可以配送的车辆
        for (Sol s : ns) {
            double surplus = parameter.getQ();
            for (customer cc : s.getCust_seq()) {
                surplus -= cc.getDemand();
            }
            if (surplus >= c.getDemand()) {
                Sol ts = new Sol();
                get_new(ts, s);
                ts.setPos(ns.indexOf(s));
                can_sol.add(ts);
                can_sol.get(can_sol.size() - 1).getCust_seq().add(c);
                flag = false;
            }
        }

        //找不到满足要求的车辆时新发一辆车
        if (flag) {
            Sol sl = new Sol();
            sl.getCust_seq().add(center);
            sl.getCust_seq().add(c);
            ns.add(sl);
        }
    }

    //互换操作
    private Sol change(Sol sol){
        Sol new_sol = new Sol();
        get_new(new_sol,sol);

        //随机选取不同的两点
        Random r = new Random();
        int x = r.nextInt(sol.getCust_seq().size()-1)+1, y;
        do {
            y = r.nextInt(sol.getCust_seq().size()-1)+1;
        }while (x == y);

        //两点互换
        Collections.swap(new_sol.getCust_seq(),x,y);

        return new_sol;
    }

    //2-opt操作
    private Sol reverse(Sol sol){
        Sol new_sol = new Sol();
        get_new(new_sol,sol);

        //随机选取不同的两点
        Random r = new Random();
        int x = r.nextInt(sol.getCust_seq().size()-1)+1, y;
        do {
            y = r.nextInt(sol.getCust_seq().size()-1)+1;
        }while (x == y);

        //倒序取出x-y之间的顾客点,重新插入
        ArrayList<customer> cus = new ArrayList<>();
        for (int i = y; i >= x; i--){
            cus.add(new_sol.getCust_seq().get(i));
        }
        for (int i = x; i <= y; i++){
            new_sol.getCust_seq().remove(i);
            new_sol.getCust_seq().add(i,cus.get(i-x));
        }

        return new_sol;
    }

    //插入操作
    private Sol insert(Sol sol){
        Sol new_sol = new Sol();
        get_new(new_sol,sol);

        //随机选取不同的两点
        Random r = new Random();
        int x = r.nextInt(sol.getCust_seq().size()-1)+1, y;
        do {
            y = r.nextInt(sol.getCust_seq().size()-1)+1;
        }while (x == y);

        //从原先的位置移除，插入新的位置
        customer c = new_sol.getCust_seq().get(x);
        new_sol.getCust_seq().remove(x);
        new_sol.getCust_seq().add(y,c);

        return new_sol;
    }

    //计算目标值
    private double cal_tarval(Sol sol){
        double driving_cost,ela_cost,fixed_cost;

        driving_cost = cal_driving_cost(sol);
        ela_cost = cal_ela_cost(sol);
        fixed_cost = cal_fixed_cost(sol);

        return driving_cost+ela_cost+fixed_cost;
    }

    //计算车辆行驶成本
    private double cal_driving_cost(Sol sol){
        double driving_cost = 0;
        double c = parameter.getC();

        for (int i=0;i<sol.getCust_seq().size()-1;i++){
            customer c1 = sol.getCust_seq().get(i);
            customer c2 = sol.getCust_seq().get(i+1);
            caldistance caldistance = new caldistance(c1.getX(),c1.getY(),c2.getX(),c2.getY());
            double dis = caldistance.distance();
            driving_cost += dis*c;
        }

        caldistance caldistance = new caldistance(sol.getCust_seq().get(sol.getCust_seq().size()-1).getX(),
                sol.getCust_seq().get(sol.getCust_seq().size()-1).getY(),
                sol.getCust_seq().get(0).getX(),sol.getCust_seq().get(0).getY());
        double d = caldistance.distance();
        driving_cost += d*c;
        return driving_cost;
    }

    //计算早到惩罚成本和迟到惩罚成本
    private double cal_ela_cost(Sol sol){
        double ea_cost = 0,la_cost = 0;
        double t0 = 10,t = t0;
        int a = parameter.getΑ();
        int b = parameter.getΒ();
        double v = getspeed.get_speed(t);
        for (int i=1;i<sol.getCust_seq().size();i++){
            customer c1 = sol.getCust_seq().get(i-1);
            customer c2 = sol.getCust_seq().get(i);
            if (c2.getId() != 0){
                caldistance caldistance = new caldistance(c1.getX(),c1.getY(),c2.getX(),c2.getY());
                double dis = caldistance.distance();
                t += dis/v*60;
                if (getspeed.get_speed(t) != v){
                    double vv = getspeed.get_speed(t);
                    double tm = getspeed.get_minutes(vv);
                    t -= dis/v*60;
                    t += tm-t+(dis-(tm-t)/60*v)/vv*60;
                    v = vv;
                }
                if (Math.max(0,c2.getE()-t) != 0){
                    ea_cost += a*Math.max(0,c2.getE()-t)/60;
                    t += Math.max(0,c2.getE()-t)+sol.getCust_seq().get(i).getSever();
                }
                else if (Math.max(0,t-c2.getL()) != 0){
                    la_cost += b*Math.max(0,t-c2.getL())/60;
                    t += sol.getCust_seq().get(i).getSever();
                }
                else {
                    t += sol.getCust_seq().get(i).getSever();
                }
            }
            else {
                t = t0;
                v = getspeed.get_speed(t);
            }
        }

        return (ea_cost+la_cost);
    }

    //计算车辆固定成本
    private double cal_fixed_cost(Sol sol){
        double fixed_cost = 0;
        double f = parameter.getF();

        for (int i=0;i<sol.getCust_seq().size();i++){
            if (sol.getCust_seq().get(i).getId() == 0){
                fixed_cost += f;
            }
        }

        return fixed_cost;
    }

    //计算温度
    private double cal_temp(Sol sol){
        double driving = 0,ea = 0,la = 0;
        double c = parameter.getC();

        for (int i=0;i<sol.getCust_seq().size()-1;i++){
            customer c1 = sol.getCust_seq().get(i);
            customer c2 = sol.getCust_seq().get(i+1);
            if (c2.getId() != 0 && c2.getId() != id){
                caldistance caldistance = new caldistance(c1.getX(),c1.getY(),c2.getX(),c2.getY());
                double dis = caldistance.distance();
                driving += dis*c;
            }
            else{
                caldistance caldistance = new caldistance(c1.getX(),c1.getY(),119.1215,36.691);
                double dis = caldistance.distance();
                driving += dis*10;
            }
        }

        caldistance caldis = new caldistance(sol.getCust_seq().get(sol.getCust_seq().size()-1).getX(),
                sol.getCust_seq().get(sol.getCust_seq().size()-1).getY(),
                119.1215,36.691);
        double d = caldis.distance();
        driving += d*10;

        double t = sol.getCust_seq().get(0).getE();
        double v = getspeed.get_speed(t);
        for (int i=1;i<sol.getCust_seq().size();i++){
            customer c1 = sol.getCust_seq().get(i-1);
            customer c2 = sol.getCust_seq().get(i);
            if (c2.getId() != 0 && c2.getId() != id){
                caldistance caldistance = new caldistance(c1.getX(),c1.getY(),c2.getX(),c2.getY());
                double dis = caldistance.distance();
                t += dis/v*60;
                if (getspeed.get_speed(t) != v){
                    double vv = getspeed.get_speed(t);
                    double tm = getspeed.get_minutes(vv);
                    t -= dis/v*60;
                    t += tm-t+(dis-(tm-t)/60*v)/vv*60;
                    v = vv;
                }
                if (Math.max(0,c2.getE()-t) != 0){
                    ea += 30*Math.max(0,c2.getE()-t)/60;
                    t += Math.max(0,c2.getE()-t)+sol.getCust_seq().get(i).getSever();
                }
                else if (Math.max(0,t-c2.getL()) != 0){
                    la += 100*Math.max(0,t-c2.getL())/60;
                    t += sol.getCust_seq().get(i).getSever();
                }
                else {
                    t += sol.getCust_seq().get(i).getSever();
                }
            }
            else {
                t = c2.getE();
                v = getspeed.get_speed(t);
            }
        }

        return driving+ea+la;
    }

    //接受新解
    private void accept_new_sol(Sol sol){
        tmp_sol = new Sol();
        for (customer c : sol.getCust_seq()){
            tmp_sol.getCust_seq().add(c);
        }
    }

    //生成最优解
    private void produce_best_sol(ArrayList<Sol> ns, ArrayList<Sol> os){
        for (int i=0;i<os.size();i++){
            for (customer c1 : os.get(i).getCust_seq()){
                best_sol.getCust_seq().add(c1);
            }
            for (int j=1;j<ns.get(i).getCust_seq().size();j++){
                best_sol.getCust_seq().add(ns.get(i).getCust_seq().get(j));
            }
        }

        //启用新车后
        if (ns.size() > os.size()){
            for (int i=os.size();i<ns.size();i++){
                for (customer c1 : ns.get(i).getCust_seq()){
                    best_sol.getCust_seq().add(c1);
                }
            }
        }
    }

    //判断解是否可行
    private boolean judge_access(Sol sol){
        //先判断是否满足车的最大载重量
        double demand = 0;
        double max = parameter.getQ();
        for (int i=0;i<sol.getCust_seq().size()-1;i++){
            demand += sol.getCust_seq().get(i).getDemand();
            if (sol.getCust_seq().get(i+1).getId() == 0
                    || sol.getCust_seq().get(i+1).getId() == id
                    || i == sol.getCust_seq().size()-2){
                if (demand > max){
                    return false;
                }
                demand = 0;
            }
        }

        //再判断时间窗是否合理
//        double t = sol.getCust_seq().get(0).getE();
//        double v = getspeed.get_speed(t);
//        for (int i=1;i<sol.getCust_seq().size();i++){
//            customer c1 = sol.getCust_seq().get(i-1);
//            customer c2 = sol.getCust_seq().get(i);
//            if (c2.getId() != 0 && c2.getId() != id){
//                caldistance caldistance = new caldistance(c1.getX(),c1.getY(),c2.getX(),c2.getY());
//                double dis = caldistance.distance();
//                t += dis/v*60;
//                if (getspeed.get_speed(t) != v){
//                    double vv = getspeed.get_speed(t);
//                    double tm = getspeed.get_minutes(vv);
//                    t -= dis/v*60;
//                    t += tm-t+(dis-(tm-t)/60*v)/vv*60;
//                    v = vv;
//                }
//                if (c2.getE()-t > 25 || t-c2.getL() > 25){
//                    return false;
//                }
//                t += sol.getCust_seq().get(i).getSever();
//            }
//            else {
//                t = c2.getE();
//                v = getspeed.get_speed(t);
//            }
//        }

        return true;
    }

    //赋值
    private void get_new(Sol s1, Sol s2){
        for (customer c : s2.getCust_seq()){
            s1.getCust_seq().add(new customer(c.getId(),c.getX(),c.getY(),
                    c.getDemand(),c.getE(),c.getL(),c.getSever()));
        }
    }

    public Sol getBest_sol() {
        return best_sol;
    }
}
