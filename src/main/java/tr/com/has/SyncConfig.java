package tr.com.has;

public class SyncConfig {

    private String myIp;
    private String mateIp;
    private int myPort;
    private int matePort;
    private int auditTimeout = 5000;
    public static final int DEFAULT_PORT = 1453;

    public SyncConfig(String myIp, String mateIp) {
        this.myIp = myIp;
        this.mateIp = mateIp;
        this.myPort = DEFAULT_PORT;
        this.matePort = DEFAULT_PORT;
    }

    public SyncConfig(String myIp, String mateIp, int myPort, int matePort) {
        this.myIp = myIp;
        this.mateIp = mateIp;
        this.myPort = myPort;
        this.matePort = matePort;
    }

    public String getMyIp() {
        return myIp;
    }

    public void setMyIp(String myIp) {
        this.myIp = myIp;
    }

    public String getMateIp() {
        return mateIp;
    }

    public void setMateIp(String mateIp) {
        this.mateIp = mateIp;
    }

    public int getMyPort() {
        return myPort;
    }

    public void setMyPort(int myPort) {
        this.myPort = myPort;
    }

    public int getMatePort() {
        return matePort;
    }

    public void setMatePort(int matePort) {
        this.matePort = matePort;
    }

    public int getAuditTimeout() {
        return auditTimeout;
    }

    public void setAuditTimeout(int auditTimeout) {
        this.auditTimeout = auditTimeout;
    }

    @Override
    public String toString() {
        return "SyncConfig{" +
                "myIp='" + myIp + '\'' +
                ", mateIp='" + mateIp + '\'' +
                ", myPort=" + myPort +
                ", matePort=" + matePort +
                ", selectTimeout=" + auditTimeout +
                '}';
    }
}
