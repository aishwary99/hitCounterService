import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;

public class WelcomeServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/html");
    PrintWriter writer = response.getWriter();
    String xForwardedForHeader = request.getHeader("X-Forwarded-For");
    if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
        String[] ips = xForwardedForHeader.split(",");
        String clientIpAddress = ips[0].trim();
        try {
            Socket socket = new Socket("192.168.122.108", 1065);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String tcpIpRequest = clientIpAddress;
            out.println(tcpIpRequest);
            
            StringBuilder responseBuilder = new StringBuilder();
            String responseLine;
            while ((responseLine = in.readLine()) != null) {
                responseBuilder.append(responseLine).append("\n");
            }
            socket.close();
            writer.println(responseBuilder.toString());
        } catch (Exception e) {
            System.out.println("Socket connection failed: " + e.getMessage());
        }
      } else {
          System.out.println("X-Forwarded-For header not found");
      }
    }
}