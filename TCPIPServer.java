import java.io.*;
import java.net.*;
import java.util.*;
import java.time.*;
import com.google.gson.*;

class SharedData {
    private Set<String> ipSet;
    private Set<String> trackerSet;
    private int hitCounter;
    private LocalDate date;
    private String hitCounterFilePath;

    public SharedData(String hitCounterFilePath) {
        this.ipSet = new HashSet<>();
        this.trackerSet = new HashSet<>();
        this.hitCounter = 0;
        this.date = LocalDate.now();
        this.hitCounterFilePath = hitCounterFilePath;
        // below method covers failover scenario
        loadData();
    }

    public void setIpSet(Set<String> ipSet) {
        this.ipSet = ipSet;
    }

    public Set<String> getIpSet() {
        return this.ipSet;
    }

    public void setTrackerSet(Set<String> trackerSet) {
        this.trackerSet = trackerSet;
    }

    public Set<String> getTrackerSet() {
        return this.trackerSet;
    }

    public void setHitCounter(int hitCounter) {
        this.hitCounter = hitCounter;
    }

    public int getHitCounter() {
        return this.hitCounter;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return this.date;
    }

    /**
     * Loads the persisted data of ip's and hitCounter in sharedData
     * Scenario - B.E Server Failover / Restarts
     */
    private void loadData() {
        String fileName = date.toString() + ".ip";
        try {
            File file = new File(fileName);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while (true) {
                    String ip = reader.readLine();
                    if (ip == null) break;
                    ipSet.add(ip.trim());
                }
                reader.close();
            }

            File hitCounterFile = new File(hitCounterFilePath);
            if (hitCounterFile.exists()) {
                FileReader fileReader = new FileReader(hitCounterFile);
                StringBuilder hitCounterBuilder = new StringBuilder();
                while (true) {
                    int hitCounterRead = fileReader.read();
                    if (hitCounterRead == -1) break;
                    hitCounterBuilder.append((char) hitCounterRead);
                }

                this.hitCounter = Integer.parseInt(hitCounterBuilder.toString().trim());
                fileReader.close();
            }
        } catch (Exception e) {}
    }
}

class FileHandler {
    public synchronized static void updateFiles(String ipFileName, int hitCounter, Set<String> ipSet, String hitCounterFile) {
        try {
            File file = new File(ipFileName);
            if (!file.exists()) file.createNewFile();
            FileWriter fileWriter = new FileWriter(file, true);
            for (String ip : ipSet) {
                fileWriter.write(ip + "\n");
            }
            fileWriter.close();

            // Update the hit counter file
            file = new File(hitCounterFile);
            if (!file.exists()) file.createNewFile();
            fileWriter = new FileWriter(file, true);
            fileWriter.write(String.valueOf(hitCounter));
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Server {
    private ServerSocket serverSocket;
    private int port;
    private String hitCounterFilePath;
    private int hitCounterFileThreshold;
    private static SharedData sharedData;

    public Server(int port, String hitCounterFilePath, int hitCounterFileThreshold) {
        this.port = port;
        this.hitCounterFilePath = hitCounterFilePath;
        this.hitCounterFileThreshold = hitCounterFileThreshold;
        this.sharedData = new SharedData(this.hitCounterFilePath);
        startListening();
    }

    private void startListening() {
        try {
            serverSocket = new ServerSocket(this.port);            
            while (true) {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(new RequestHandler(socket));
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class RequestHandler implements Runnable {
        private Socket socket;

        public RequestHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String ipAddress = reader.readLine();

                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                
                boolean isDumpRequired = false;
                String fileName = "";
                Set<String> tempIpSet = null;
                int currentHitCounter = 0;

                synchronized(sharedData) {
                    Set<String> ipSet = sharedData.getIpSet();
                    Set<String> trackerSet = sharedData.getTrackerSet();
                    int hitCounter = sharedData.getHitCounter();
                    fileName = sharedData.getDate().toString() + ".ip";

                    LocalDate currentDate = LocalDate.now();
                    if (currentDate.toString().equalsIgnoreCase(sharedData.getDate().toString())) {
                        if (!ipSet.contains(ipAddress)) {
                            ipSet.add(ipAddress);
                            trackerSet.add(ipAddress);
                            hitCounter++;
                        }
                        
                        if (trackerSet.size() == hitCounterFileThreshold) {
                            isDumpRequired = true;
                            tempIpSet = new HashSet<>(trackerSet);
                            trackerSet.clear();
                        }
                    } else {
                        isDumpRequired = true;
                        tempIpSet = new HashSet<>(trackerSet);
                        trackerSet.clear();

                        sharedData.setIpSet(new HashSet<String>());
                        sharedData.setTrackerSet(new HashSet<String>());
                        ipSet = sharedData.getIpSet();
                        ipSet.add(ipAddress);
                        sharedData.setDate(currentDate);
                        hitCounter++;
                        fileName = currentDate.toString() + ".ip";
                    }
                    sharedData.setHitCounter(hitCounter);
                    currentHitCounter = hitCounter;
                }
                writer.println(currentHitCounter);
                socket.close();
            
                if (isDumpRequired) {
                    FileHandler.updateFiles(fileName, currentHitCounter, tempIpSet, hitCounterFilePath);
                    tempIpSet.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

public class TCPIPServer {
    private static ServerConfigurations getServerConfigurations() {
        String configFilePath = "config/conf.json";
        ServerConfigurations serverConfigurations = null;
        try {
            FileReader reader = new FileReader(configFilePath);
            Gson gson = new Gson();
            serverConfigurations = gson.fromJson(reader, ServerConfigurations.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serverConfigurations;
    }

    public static void main(String[] args) {
        ServerConfigurations serverConfigurations = getServerConfigurations();
        Server server = new Server(serverConfigurations.getPort(), serverConfigurations.getHitCounterFilePath(), serverConfigurations.getHitCounterFileThreshold());
    }
}