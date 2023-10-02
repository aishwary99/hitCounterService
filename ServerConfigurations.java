public class ServerConfigurations {
    private int port;
    private String hitCounterFilePath;
    private int hitCounterFileThreshold;

    public ServerConfigurations() {}

    public ServerConfigurations(int port, String hitCounterFilePath, int hitCounterFileThreshold) {
        this.port = port;
        this.hitCounterFilePath = hitCounterFilePath;
        this.hitCounterFileThreshold = hitCounterFileThreshold;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHitCounterFilePath() {
        return this.hitCounterFilePath;
    }

    public void setHitCounterFilePath(String hitCounterFilePath) {
        this.hitCounterFilePath = hitCounterFilePath;
    }

    public int getHitCounterFileThreshold() {
        return this.hitCounterFileThreshold;
    }

    public void setHitCounterFileThreshold(int hitCounterFileThreshold) {
        this.hitCounterFileThreshold = hitCounterFileThreshold;
    }

    public String toString() {
        return "ServerConfigurations{" +
                "port=" + this.port +
                "hitCounterFilePath=" + this.hitCounterFilePath +
                "hitCounterFileThreshold=" + this.hitCounterFileThreshold +
                '}';
    }
}