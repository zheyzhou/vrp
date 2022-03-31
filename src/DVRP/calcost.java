package DVRP;

public class calcost {
    private final parameter parameter = new parameter();
    private final getspeed getspeed = new getspeed();

    //计算目标值
    public double cal_tarval(Sol sol){
        double driving_cost,ela_cost,fixed_cost;

        driving_cost = cal_driving_cost(sol);
        ela_cost = cal_ela_cost(sol);
        fixed_cost = cal_fixed_cost();

        return driving_cost+ela_cost+fixed_cost;
    }

    //计算车辆行驶成本
    public double cal_driving_cost(Sol sol){
        double driving_cost = 0;
        double c = parameter.getC();

        for (int i=0;i<sol.getCust_seq().size()-1;i++){
            customer c1 = sol.getCust_seq().get(i);
            customer c2 = sol.getCust_seq().get(i+1);
            caldistance caldistance = new caldistance(c1.getX(),c1.getY(),c2.getX(),c2.getY());
            double dis = caldistance.distance();
            driving_cost += dis*c;
        }

        return driving_cost;
    }

    //计算早到惩罚成本和迟到惩罚成本
    public double cal_ela_cost(Sol sol){
        double ea_cost = 0,la_cost = 0;
        double t0 = 10,t = t0;

        int a = parameter.getΑ();
        int b = parameter.getΒ();

        double v = getspeed.get_speed(t);

        for (int i=1;i<sol.getCust_seq().size()-1;i++){

            customer c1 = sol.getCust_seq().get(i-1);
            customer c2 = sol.getCust_seq().get(i);

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
                sol.setEarly_time(sol.getEarly_time()+c2.getE()-t);
                t += Math.max(0,c2.getE()-t)+sol.getCust_seq().get(i).getSever();
            }
            else if (Math.max(0,t-c2.getL()) != 0){
                la_cost += b*Math.max(0,t-c2.getL())/60;
                sol.setLate_time(sol.getLate_time()+t-c2.getL());
                t += sol.getCust_seq().get(i).getSever();
            }
            else {
                t += sol.getCust_seq().get(i).getSever();
            }
        }

        return (ea_cost+la_cost);
    }

    //计算车辆固定成本
    public double cal_fixed_cost(){
        return parameter.getF();
    }
}
