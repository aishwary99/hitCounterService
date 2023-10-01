public class ServerConfigurations {
    private int port;

    public ServerConfigurations() {}

    public ServerConfigurations(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String toString() {
        return "ServerConfigurations{" +
                "port=" + this.port +
                '}';
    }
}