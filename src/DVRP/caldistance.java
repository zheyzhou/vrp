package DVRP;

//计算两点间的距离
public class caldistance {
    private final double x1,y1,x2,y2;

    public caldistance(double x1,double y1,double x2,double y2){
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double distance(){
        double EARTH_RADIUS = 6378.137;
        double radLat1 = Deg2Rad(x1);
        double radLat2 = Deg2Rad(x2);
        double a = radLat1 - radLat2;
        double b = Deg2Rad(y1) - Deg2Rad(y2);
        double s = 2*Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2)+
                Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s*10000)/10000;//保留四位小数
        return s;
    }

    private double Deg2Rad(double d){
        return d*Math.PI/180;
    }
}
