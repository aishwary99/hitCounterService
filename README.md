# Hit Counter Service

## Overview 
The Hit Counter Service is a highly scalable framework designed to provide hit counts for web applications. This service calculates hit counts based on the number of unique IP addresses accessing a website on a daily basis. It offers a robust solution for tracking website traffic and user engagement.

### Features
* Scalability: The framework is built to handle high volumes of traffic, making it suitable for web applications of all sizes.

* Daily Tracking: The service accurately tracks unique IP addresses on a per-day basis, providing valuable insights into website usage patterns.
    
### Prerequisites

The things you need before installing the software.

* Two Virtual Machines [Minimum].
* Java v1.8+ on both the VM's.
* Apache tomcat in VM-1 [/usr/share/tomcat for linux].
* Firewall rules need to be configured to allow incoming/outgoing connections on specific ports.
* Servlet Mapping in deployment descriptor file for HitCounterService.
* Conf.json to be configured with specific ports, hitCounterFile and hitCounterFileThreshold aka dumpThreshold. 

### Installation

This guide outlines the steps required to set up the Hit Counter Service within your web application. Follow these instructions to seamlessly integrate hit counting functionality into your project.

###### Virtual Machine One

1. Download the Jar File: Download the HitCounterService.jar file and place it in the lib folder of your project, which should be forked from the VM-1 repository.
2. Configure web.xml:
    - Modify your web application's web.xml file to include servlet mappings for the Hit Counter Service.
    - Ensure that the web.xml contains init-param configurations for the "server-host" and "server-port" fields to establish a connection with the TCPIPServer.
     ```
     <servlet>
    <servlet-name>com.thinking.machines.HitCounterService</servlet-name>
    <servlet-class>com.thinking.machines.HitCounterService</servlet-class>
    <init-param>
        <param-name>server-host</param-name>
        <param-value>192.168.122.108</param-value>
    </init-param>
    <init-param>
        <param-name>server-port</param-name>
        <param-value>1065</param-value>
    </init-param>
    </servlet>
      <servlet-mapping>
        <servlet-name>com.thinking.machines.HitCounterService</servlet-name>
        <url-pattern>/api/getHitCounter</url-pattern>
      </servlet-mapping>
     ```
     
     
3. JavaScript File:
    - Obtain the visitor-counter.js JavaScript code, which contains the necessary logic to interact with the Hit Counter Service endpoint and retrieve hit counts.
    - Ensure you fork it from VM-1 Thinking Machine's folder.
  
4. Index.html Configuration:
    - Modify your index.html file as shown below to include the hit counter functionality:
  
   ```
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Your Web Application</title>
        <link rel="stylesheet" href="/your-web-app/static/css/style.css" />
        <script src="/your-web-app/static/js/visitor-counter.js"></script>
    </head>
    <body>
        <header>
            <h1>Your Web Application</h1>
        </header>
        <visitor-number uri="/your-web-app/api/getHitCounter"></visitor-number>
        <footer>
            <p>&copy; 2023 Your Web Application. All rights reserved.</p>
        </footer>
    </body>
    </html>
   ```
   - Make sure to replace /your-web-app with the appropriate path for your web application.
   - Include the <visitor-number> tag with the corresponding URI of the hit counter endpoint to fetch and display the hit counter.
  
###### Virtual Machine Two

1. TCPIPServer.jar - You must have the TCPIPServer.jar in the working directory's lib folder, which should be forked from the VM-2 branch of the repository.
2. Configuration - To configure the TCPIPServer service, you need to create a conf.json file in the config folder of your working directory. The conf.json file should contain the following JSON fields:
```
{
    "port": 1065,
    "hitCounterFilePath": "hitCounter.txt",
    "hitCounterFileThreshold": 5
}
```
3. Libraries - You need the gson-2.8.8.jar library to parse the conf.json file. Place the gson-2.8.8.jar file in the lib folder of your working directory.

### Branches

* Main
* Feature : VM-1 and VM-2
