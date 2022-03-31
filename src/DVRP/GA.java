package DVRP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class GA {
    private final int popsize = 100;//种群规模
    private final double Pc = 0.90;//交叉概率
    private final double Pm = 0.10;//变异概率
    private customer tmp_cust;
    private final Sol best_sol = new Sol();
    private final Sol tmp_pop = new Sol();
    private final Sol tmp_sol = new Sol();
    private final ArrayList<Sol> pops = new ArrayList<>();//种群
    private final ArrayList<Sol> sols = new ArrayList<>();//种群对应的解
    private final parameter parameter = new parameter();//车辆参数
    private final getspeed getspeed = new getspeed();//不同时段的车速

    public void start(Sol sol){
        tmp_cust = sol.getCust_seq().get(sol.getCust_seq().size()-1);
        for (customer c : sol.getCust_seq()){
            tmp_pop.getCust_seq().add(new customer(c.getId(),c.getX(),c.getY(),
                    c.getDemand(),c.getE(),c.getL(),c.getSever()));
        }

        //生成初始种群
        while(pops.size() != popsize){
            random();
            gen_path();
        }

        //计算初始种群的适应度
        for (int i=0;i<sols.size();i++){
            cal_fitness(sols.get(i));
            pops.get(i).setFit(sols.get(i).getFit());
            pops.get(i).setCost(sols.get(i).getCost());
        }

        //迭代
        int num = 0;
        while (num < 800){
            //选择前20%
            choose();

            //繁殖下一代
            while (sols.size() <= popsize){
                double r = Math.random();
                //0.9的概率发生交叉
                if (r <= Pc){
                    overlapping();
                }
                //0.1的概率发生变异
                if (r > Pc && r <= Pc+Pm){
                    variation();
                }
            }

            //上一代最优解和这一代最优解相比较
            Sol t = new Sol();
            get_new(t, best_sol);
            get_best();

            //查看这一代最优解与上一代最优解是否相等
            if (t.getCust_seq().size() != best_sol.getCust_seq().size()){
                num = 0;
            }
            else {
                boolean flag = true;
                for (int i=0;i<t.getCust_seq().size();i++){
                    if (t.getCust_seq().get(i).getId() != best_sol.getCust_seq().get(i).getId()){
                        num = 0;
                        flag = false;
                        break;
                    }
                }
                if (flag){
                    num++;
                }
            }
        }

        //得到最优解
        get_best();
    }

    //随机交换
    private void random(){
        Random r = new Random();
        for (int i=0;i<20;i++){
            int x = r.nextInt(20);
            int y = r.nextInt(20);
            Collections.swap(tmp_pop.getCust_seq(),x,y);
        }
        if (judge()){
            random();
        }
    }

    //判断是否重复
    private boolean judge(){
        for (Sol sol : pops){
            boolean flag = true;
            for (int i=0;i<sol.getCust_seq().size();i++){
                if (sol.getCust_seq().get(i).getId() != tmp_pop.getCust_seq().get(i).getId()){
                    flag = false;
                    break;
                }
            }
            if (flag){
                return true;
            }
        }
        return false;
    }

    //生成解
    private void gen_path(){
        int Q = parameter.getQ();
        double sum_de = 0;
        Sol tmp_a = new Sol();
        get_new(tmp_a, tmp_pop);

        //生成解，满足车辆载重的放一起
        tmp_sol.getCust_seq().add(tmp_cust);
        for (int i=0;i<tmp_pop.getCust_seq().size();i++){
            if (tmp_pop.getCust_seq().get(i).getDemand() + sum_de <= 0.9*Q){
                tmp_sol.getCust_seq().add(tmp_pop.getCust_seq().get(i));
                sum_de += tmp_pop.getCust_seq().get(i).getDemand();
            }
            else {
                tmp_sol.getCust_seq().add(tmp_cust);
                sum_de = 0;
                i--;
            }
        }

        Sol tmp_b = new Sol();
        get_new(tmp_b, tmp_sol);
        pops.add(tmp_a);
        sols.add(tmp_b);
        tmp_sol.setCust_seq(new ArrayList<>());
    }

    //判断解是否满足载重
    private boolean judge_load(Sol sol){
        int Q = parameter.getQ();
        double sum = 0;

        for (customer c : sol.getCust_seq()){
            sum += c.getDemand();
            if (c.getId() == 0){
                if (sum > Q){
                    return false;
                }
                else {
                    sum = 0;
                }
            }
        }

        return true;
    }

    //计算适应度
    private void cal_fitness(Sol sol){
        double total_cost = cal_tarval(sol);
        double F = Math.pow(10,12)*Math.pow(total_cost,-Math.log(total_cost));
        sol.setCost(total_cost);
        sol.setFit(F);
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

    //赋值
    private void get_new(Sol s1, Sol s2){
        for (customer c : s2.getCust_seq()){
            s1.getCust_seq().add(new customer(c.getId(),c.getX(),c.getY(),
                    c.getDemand(),c.getE(),c.getL(),c.getSever()));
        }
    }

    //选择
    private void choose(){
        //计算总适应度
        double f = 0;
        for (Sol item : sols) {
            f += item.getFit();
        }
        //计算每个个体的概率
        for (Sol value : sols) {
            value.setP(value.getFit() / f);
        }
        //计算每个个体的累积概率
        for (int i=0;i<sols.size()-1;i++){
            double P = 0;
            for (int j=0;j<=i;j++){
                P += sols.get(j).getP();
            }
            sols.get(i).setP0(P);
        }
        sols.get(sols.size()-1).setP0(1);
        //将种群数组也赋值
        for (int i=0;i<sols.size();i++){
            pops.get(i).setP(sols.get(i).getP());
            pops.get(i).setP0(sols.get(i).getP0());
        }
        //选择符合条件的个体
        ArrayList<Sol> tmp_pops = new ArrayList<>();
        ArrayList<Sol> tmp_sols = new ArrayList<>();
        for (int i=0;i<sols.size()/5;i++){
            double r = Math.random();
            if (r < sols.get(0).getP0()){
                tmp_pops.add(pops.get(0));
                tmp_sols.add(sols.get(0));
            }
            else {
                for (int j=0;j<sols.size()-1;j++){
                    if (sols.get(j).getP0() < r && sols.get(j+1).getP0() > r){
                        tmp_pops.add(pops.get(j+1));
                        tmp_sols.add(sols.get(j+1));
                        break;
                    }
                }
            }
        }

        //用原种群中适应度最大的代替新种群中适应度最小的
        //找出适应度最大的
        Sol s = new Sol(new ArrayList<>());
        for (customer c : sols.get(0).getCust_seq()){
            s.getCust_seq().add(c);
        }
        s.setFit(sols.get(0).getFit());
        s.setCost(sols.get(0).getCost());
        Sol p = new Sol(new ArrayList<>());
        for (customer c : pops.get(0).getCust_seq()){
            p.getCust_seq().add(c);
        }
        p.setFit(pops.get(0).getFit());
        p.setCost(pops.get(0).getCost());

        for (Sol item : sols) {
            if (item.getFit() > s.getFit()){
                s.getCust_seq().clear();
                for (customer c : item.getCust_seq()){
                    s.getCust_seq().add(c);
                }
                s.setFit(item.getFit());
                s.setCost(item.getCost());
            }
        }
        for (Sol value : pops) {
            if (value.getFit() > p.getFit()){
                p.getCust_seq().clear();
                for (customer c : value.getCust_seq()){
                    p.getCust_seq().add(c);
                }
                p.setFit(value.getFit());
                p.setCost(value.getCost());
            }
        }

        //找出适应度最小的
        int n = 0;
        Sol s1 = tmp_sols.get(0);
        for (int i=0;i<tmp_sols.size();i++){
            if (s1.getFit() > tmp_sols.get(i).getFit()){
                s1 = tmp_sols.get(i);
                n = i;
            }
        }
        //替换
        tmp_pops.set(n,p);
        tmp_sols.set(n,s);
        //用新的种群覆盖旧种群
        pops.clear();
        sols.clear();
        pops.addAll(tmp_pops);
        sols.addAll(tmp_sols);
    }

    //交叉
    private void overlapping(){
        //随机选择两个解
        Random r = new Random();
        int x = r.nextInt(pops.size());
        int y = r.nextInt(pops.size());
        Sol tmp1 = new Sol(new ArrayList<>());
        for (customer c : pops.get(x).getCust_seq()){
            tmp1.getCust_seq().add(c);
        }
        Sol tmp2 = new Sol(new ArrayList<>());
        for (customer c : pops.get(y).getCust_seq()){
            tmp2.getCust_seq().add(c);
        }
        Sol tmp11 = new Sol(new ArrayList<>());
        for (customer c : sols.get(x).getCust_seq()){
            tmp11.getCust_seq().add(c);
        }
        Sol tmp22 = new Sol(new ArrayList<>());
        for (customer c : sols.get(y).getCust_seq()){
            tmp22.getCust_seq().add(c);
        }

        //在两个解中随机各选择一条路径
        int n = cal_paths(tmp11);
        ArrayList<Integer> p = choose_path(tmp11,n);
        ArrayList<Integer> a = new ArrayList<>(p);
        n = cal_paths(tmp22);
        p = choose_path(tmp22,n);
        ArrayList<Integer> b = new ArrayList<>(p);

        //相对应的删除选择的路径
        ArrayList<customer> c1 = new ArrayList<>();
        ArrayList<customer> c2 = new ArrayList<>();
        del_path(tmp11,b,c1);
        del_path(tmp22,a,c2);

        //把删除的点再重新插入
        tmp11 = insert_path(tmp11,c1);
        tmp22 = insert_path(tmp22,c2);


        //更新pops和sols
        sols.add(tmp11);
        sols.add(tmp22);
        tmp1 = update_pop(tmp11);
        tmp2 = update_pop(tmp22);
        pops.add(tmp1);
        pops.add(tmp2);
    }

    //变异
    private void variation(){
        Random r = new Random();

        //随机选择一个解
        int a = r.nextInt(sols.size());
        Sol tmp_a = new Sol();
        get_new(tmp_a, sols.get(a));

        //随机选择该解中两个点互换
        do {
            int x = r.nextInt(tmp_a.getCust_seq().size());
            while (tmp_a.getCust_seq().get(x).getId() == 0){
                x = r.nextInt(tmp_a.getCust_seq().size());
            }
            int y = r.nextInt(tmp_a.getCust_seq().size());
            while (tmp_a.getCust_seq().get(y).getId() == 0){
                y = r.nextInt(tmp_a.getCust_seq().size());
            }
            Collections.swap(tmp_a.getCust_seq(),x,y);
        }while (!judge_load(tmp_a));

        //保存新解并计算其适应度
        sols.add(tmp_a);
        cal_fitness(sols.get(sols.size()-1));

        //生成对应种群
        Sol tmp_b = new Sol();
        for (customer c : tmp_a.getCust_seq()){
            if (c.getId() != 0){
                tmp_b.getCust_seq().add(c);
            }
        }

        ////保存新种群及其适应度
        pops.add(tmp_b);
        pops.get(sols.size()-1).setFit(sols.get(sols.size()-1).getFit());
        pops.get(sols.size()-1).setCost(sols.get(sols.size()-1).getCost());
    }

    //统计路径数
    private int cal_paths(Sol s){
        int n = 0;
        for (int i=0;i<s.getCust_seq().size();i++) {
            if (s.getCust_seq().get(i).getId() == 0) {
                n++;
            }
        }
        return n;
    }

    //随机选择一条路径
    private ArrayList<Integer> choose_path(Sol s, int n){
        Random r = new Random();
        int z = r.nextInt(n)+1;
        int t = 0;
        int flag = 0;
        ArrayList<Integer> p = new ArrayList<>();
        for (int i=0;i<s.getCust_seq().size();i++){
            if (s.getCust_seq().get(i).getId() == 0){
                t++;
            }
            if (t == z){
                for (int j=i+1;j<s.getCust_seq().size();j++){
                    if (s.getCust_seq().get(j).getId() == 0){
                        break;
                    }
                    p.add(s.getCust_seq().get(j).getId());
                }
                flag = 1;
            }
            if (flag == 1){
                break;
            }
        }
        return p;
    }

    //删除相对应的路径
    private void del_path(Sol s,ArrayList<Integer> p,ArrayList<customer> c){
        for (int i=0;i<s.getCust_seq().size();i++){
            for (int j=0;j<p.size();j++) {
                if (s.getCust_seq().get(i).getId() == p.get(j)) {
                    c.add(s.getCust_seq().get(i));
                    s.getCust_seq().remove(i);
                    p.remove(j);
                    i--;
                    break;
                }
            }
        }

        //去掉空路径
        for (int i=0;i<s.getCust_seq().size()-1;i++){
            if (s.getCust_seq().get(i).getId() == 0 && s.getCust_seq().get(i+1).getId() == 0){
                s.getCust_seq().remove(i);
                i--;
            }
        }
        if (s.getCust_seq().get(s.getCust_seq().size()-1).getId() == 0){
            s.getCust_seq().remove(s.getCust_seq().size()-1);
        }
    }

    //重新插入对应解
    private Sol insert_path(Sol s,ArrayList<customer> c){
        //把所有路径独立出来
        ArrayList<Sol> ss = new ArrayList<>();
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

        //插入后是否满足容量要求
        int Q = parameter.getQ();
        for (DVRP.customer customer : c) {
            //先判断能否插入，并选出能插入的路径
            ArrayList<Sol> ts = new ArrayList<>();
            for (Sol sol : ss) {
                double sum_de = customer.getDemand();
                for (int k = 0; k < sol.getCust_seq().size(); k++) {
                    sum_de += sol.getCust_seq().get(k).getDemand();
                }
                if (sum_de <= Q) {
                    ts.add(sol);
                }
            }

            //找不到能插入的路径时新建一条路径
            if (ts.size() == 0){
                Sol t_sol = new Sol();
                t_sol.getCust_seq().add(ss.get(0).getCust_seq().get(0));
                t_sol.getCust_seq().add(customer);
                ss.add(t_sol);
            }

            //找到路径时寻找最佳插入位置
            else {
                double min_tar = 100000;
                int a = 0,b = 0;
                for (int i = 0; i < ts.size(); i++) {
                    for (int j = 1; j < ts.get(i).getCust_seq().size(); j++) {
                        ts.get(i).getCust_seq().add(j,customer);
                        double m = cal_tarval(ts.get(i));
                        if (m < min_tar){
                            min_tar = m;
                            a = i;
                            b = j;
                        }
                        ts.get(i).getCust_seq().remove(j);
                    }
                }
                ts.get(a).getCust_seq().add(b,customer);
            }
        }

        //将所有点插入后重建新解
        Sol t_s = new Sol();
        for (Sol sol : ss) {
            for (customer cc : sol.getCust_seq()) {
                t_s.getCust_seq().add(cc);
            }
        }
        s = t_s;

        //去掉空路径
        for (int i=0;i<s.getCust_seq().size()-1;i++){
            if (s.getCust_seq().get(i).getId() == 0 && s.getCust_seq().get(i+1).getId() == 0){
                s.getCust_seq().remove(i);
                i--;
            }
        }

        //计算新解的适应度
        cal_fitness(s);

        return s;
    }

    //更新pops
    private Sol update_pop(Sol s){
        Sol ss = new Sol();
        for (customer c : s.getCust_seq()){
            if (c.getId() != 0){
                ss.getCust_seq().add(c);
            }
        }
        ss.setFit(s.getFit());
        ss.setCost(s.getCost());
        return ss;
    }

    //得到最优解
    private void get_best(){
        best_sol.getCust_seq().clear();
        get_new(best_sol, sols.get(0));
        best_sol.setFit(sols.get(0).getFit());
        best_sol.setCost(sols.get(0).getCost());
        for (Sol sol : sols){
            if (best_sol.getCost() > sol.getCost()){
                best_sol.getCust_seq().clear();
                get_new(best_sol, sol);
                best_sol.setFit(sol.getFit());
                best_sol.setCost(sol.getCost());
            }
        }
    }

    public Sol getBest_sol() {
        return best_sol;
    }
}
