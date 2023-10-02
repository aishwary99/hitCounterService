package com.thinking.machines;

import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;

public class HitCounterService extends HttpServlet {
    private String serverHost;
    private int serverPort;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        // get the init-param values from web.xml
        this.serverHost = servletConfig.getInitParameter("server-host");
        this.serverPort = Integer.parseInt(servletConfig.getInitParameter("server-port"));
    }

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
            // TCP-IP Client code starts here
            Socket socket = new Socket(this.serverHost, this.serverPort);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream, true);
            printWriter.println(clientIpAddress);


            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            StringBuilder responseBuilder = new StringBuilder();
            int bytesRead;
            while (true) {
                bytesRead = inputStreamReader.read();
                if (bytesRead == -1) break;
                responseBuilder.append((char) bytesRead);
            }
            
            socket.close();
            writer.println(responseBuilder.toString());
            // TCP-IP Client code ends here
        } catch (Exception e) {}
      }
    }
}