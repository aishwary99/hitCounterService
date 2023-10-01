import java.net.*;

public class Sample {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("192.168.122.108", 1065); // Replace with your target server and port
            System.out.println("Socket connection successful.");
            socket.close();
        } catch (Exception e) {
            System.err.println("Socket connection failed: " + e.getMessage());
        }
    }
}
