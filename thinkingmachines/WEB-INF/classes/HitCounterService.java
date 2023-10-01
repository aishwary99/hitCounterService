import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;

public class HitCounterService extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // set response headers
    response.setContentType("text/html");
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
    response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    
    PrintWriter writer = response.getWriter();
    // Fetch xForwardedForHeader from request header
    String xForwardedForHeader = request.getHeader("X-Forwarded-For");
    if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
        // Split the header value , 0th index will contain the client's ip
        String[] ips = xForwardedForHeader.split(",");
        String clientIpAddress = ips[0].trim();
        try {
            // IP Address and Port number is configurable
            String serverHost = getConfigValue("tcp-ip-server-host");
            int port = Integer.parseInt(getConfigValue("tcp-ip-server-port"));

            // TCP-IP Client code starts here
            Socket socket = new Socket(serverHost, port);
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String tcpIpRequest = clientIpAddress;
            printWriter.println(tcpIpRequest);
            
            StringBuilder responseBuilder = new StringBuilder();
            String responseLine;
            while ((responseLine = bufferedReader.readLine()) != null) {
                responseBuilder.append(responseLine).append("\n");
            }
            socket.close();
            writer.println(responseBuilder.toString());
            // TCP-IP Client code ends here
        } catch (Exception e) {}
      }
    }

    /*
    * Utility method to parse the configKey from web.xml file contents
    * and return its value as String
    */
    private String getConfigValue(String configKey) {
        String configKeyOpeningTag = "<" + configKey + ">";
        String configKeyClosingTag = "</" + configKey + ">";
        String webXMLPath = "/WEB-INF/web.xml";
        try {
            ServletContext servletContext = getServletContext();
            InputStream webXmlStream = servletContext.getResourceAsStream(webXMLPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(webXmlStream));
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) break;
                if (line.contains(configKeyOpeningTag)) {
                    int startIndex = line.indexOf(configKeyOpeningTag) + configKeyOpeningTag.length();
                    int endIndex = line.indexOf(configKeyClosingTag);
                    return line.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {}
        return "";
    }
}