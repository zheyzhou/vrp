package DVRP;

import java.util.ArrayList;

//种群信息
public class Sol {
    private ArrayList<customer> cust_seq = new ArrayList<>();
    private double fit;//适应度
    private double cost;//总成本
    private double p;//遗传到下一代种群的概率
    private double P0;//累积概率
    private int pos;//解的位置
    private double early_time = 0;//早到时长
    private double late_time = 0;//迟到时长

    public Sol(){

    }

    public Sol(ArrayList<customer> cust_seq){
        this.cust_seq = cust_seq;
    }

    public ArrayList<customer> getCust_seq() {
        return cust_seq;
    }

    public void setCust_seq(ArrayList<customer> cust_seq) {
        this.cust_seq = cust_seq;
    }

    public double getFit() {
        return fit;
    }

    public void setFit(double fit) {
        this.fit = fit;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public double getP0() {
        return P0;
    }

    public void setP0(double p0) {
        P0 = p0;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public double getEarly_time() {
        return early_time;
    }

    public void setEarly_time(double early_time) {
        this.early_time = early_time;
    }

    public double getLate_time() {
        return late_time;
    }

    public void setLate_time(double late_time) {
        this.late_time = late_time;
    }
}
