package data;

public class TrajectoryData {
    private int pid;
    private int bid;
    private double usep;
    private double hmov; // Horizontal Movement in inches (從資料庫讀取)
    private double vmov; // Vertical Movement in inches (從資料庫讀取)
    private double rex;
    private double rey;
    private double speed; // Release Speed in mph (從資料庫讀取)

    public TrajectoryData(int pid, int bid, double usep, double hmov, double vmov, double rex, double rey, double speed) {
        this.pid = pid;
        this.bid = bid;
        this.usep = usep;
        this.hmov = hmov;
        this.vmov = vmov;
        this.rex = rex;
        this.rey = rey;
        this.speed = speed;
    }

    // Getter 方法
    public int getPid() { return pid; }
    public int getBid() { return bid; }
    public double getUsep() { return usep; }
    public double getHmov() { return hmov; }
    public double getVmov() { return vmov; }
    public double getRex() { return rex; }
    public double getRey() { return rey; }
    public double getSpeed() { return speed; }
}