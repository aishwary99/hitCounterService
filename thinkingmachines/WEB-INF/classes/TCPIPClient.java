import java.io.*;
import java.net.*;
import java.util.*;

public class TCPIPClient {
    private static String generateRandomIPAddress() {
        Random random = new Random();
        int octet1 = random.nextInt(256); // Range: 0-255
        int octet2 = random.nextInt(256);
        int octet3 = random.nextInt(256);
        int octet4 = random.nextInt(256);
        
        return String.format("%d.%d.%d.%d", octet1, octet2, octet3, octet4);
    }

    public static void main(String[] args) {
        String serverIP = "192.168.122.108"; // Change to the server's IP if needed
        int serverPort = 1065;

        try (Socket socket = new Socket(serverIP, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String randomIP = TCPIPClient.generateRandomIPAddress();
            System.out.println("Sending IP to server: " + randomIP);
            out.println(randomIP);

            int response = Integer.parseInt(in.readLine());
            System.out.println("Received response from server: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}