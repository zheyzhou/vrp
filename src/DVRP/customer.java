package DVRP;

import java.sql.Time;

//顾客点信息
public class customer {
    private int id;
    private double x,y,demand,sever,e,l;
    private Time t1,t2;

    public customer(){}

    public customer(int id,double x,double y,double demand,double e,double l,double sever){
        this.id = id;
        this.x = x;
        this.y = y;
        this.demand = demand;
        this.e = e;
        this.l = l;
        this.sever = sever;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getDemand() {
        return demand;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public double getE() {
        return e;
    }

    public double getL() {
        return l;
    }

    public void setT1(Time t1) {
        this.t1 = t1;
        int h = (this.t1.getHours()+16)<24 ? this.t1.getHours()+16 : this.t1.getHours()-8;
        this.t1.setHours(h);
        e = (this.t1.getHours()-8)*60+this.t1.getMinutes()+this.t1.getSeconds()/60;
    }

    public void setT2(Time t2) {
        this.t2 = t2;
        int h = (this.t2.getHours()+16)<24 ? this.t2.getHours()+16 : this.t2.getHours()-8;
        this.t2.setHours(h);
        l = (this.t2.getHours()-8)*60+this.t2.getMinutes()+this.t2.getSeconds()/60;
    }

    public double getSever() {
        return sever;
    }

    public void setSever(double sever) {
        this.sever = sever;
    }
}
