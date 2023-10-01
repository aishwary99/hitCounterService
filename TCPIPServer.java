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

    public SharedData() {
        this.ipSet = new HashSet<>();
        this.trackerSet = new HashSet<>();
        this.hitCounter = 0;
        this.date = LocalDate.now();
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
            // Load the contents of IP file to ipSet
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

            // Load the hit counter from the hitCounter file
            File hitCounterFile = new File(FileHandler.HIT_COUNTER_FILE);
            if (hitCounterFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(hitCounterFile));
                String hitCounterRead = reader.readLine();
                if (hitCounterRead != null) {
                    hitCounter = Integer.parseInt(hitCounterRead);
                }
                reader.close();
            }
            
            /* - shared this idea in video demonstration : not required if
            // we are ok to loose some records from trackerSet during downtime
            // although : those set of records are already considered in ipSet and hitCounter
            // Load the contents of tracker set file to tracker set
            fileName = date.toString() + "-tracker-set" + ".ip";
            File trackerSetFile = new File(fileName);
            if (trackerSetFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(trackerSetFile));
                while (true) {
                    String ip = reader.readLine();
                    if (ip == null) break;
                    trackerSet.add(ip.trim());
                }
                reader.close();
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class FileHandler {
    // it can be made configurable later
    public static final String HIT_COUNTER_FILE = "hitCounter.txt";
    public static final int DUMP_THRESHOLD = 5;

    public synchronized static void updateFiles(String ipFileName, int hitCounter, Set<String> ipSet) {
        try {
            // Update the IP file
            File file = new File(ipFileName);
            if (!file.exists()) file.createNewFile();
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file, true));
            for (String ip : ipSet) {
                fileWriter.write(ip + "\n");
            }
            fileWriter.close();

            // Update the hit counter file
            file = new File(HIT_COUNTER_FILE);
            if (!file.exists()) file.createNewFile();
            fileWriter = new BufferedWriter(new FileWriter(file, false));
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
    private static SharedData sharedData;

    public Server(int port) {
        this.port = port;
        this.sharedData = new SharedData();
        startListening();
    }

    private void startListening() {
        try {
            serverSocket = new ServerSocket(port);            
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                String ipAddress = reader.readLine();
                
                boolean isDumpRequired = false;
                String fileName = "";
                Set<String> tempIpSet = null;
                int currentHitCounter = 0;

                // acquiring lock on sharedData object
                synchronized(sharedData) {
                    Set<String> ipSet = sharedData.getIpSet();
                    Set<String> trackerSet = sharedData.getTrackerSet();
                    int hitCounter = sharedData.getHitCounter();
                    fileName = sharedData.getDate().toString() + ".ip";

                    // compare the date : if the date is same - check the entry in ipSet
                    // if ipSet doesn't contain - consider it and increase the hitCounter
                    LocalDate currentDate = LocalDate.now();
                    if (currentDate.toString().equalsIgnoreCase(sharedData.getDate().toString())) {
                        if (!ipSet.contains(ipAddress)) {
                            ipSet.add(ipAddress);
                            trackerSet.add(ipAddress);
                            hitCounter++;
                        }
                        
                        // if tracker set exhausts
                        if (trackerSet.size() == FileHandler.DUMP_THRESHOLD) {
                            // dump the data of tracker set in the file
                            isDumpRequired = true;
                            tempIpSet = new HashSet<>(trackerSet);
                            trackerSet.clear();
                        }
                    } else {
                        // if control comes here, it means - the date is changed
                        isDumpRequired = true;
                        tempIpSet = new HashSet<>(trackerSet);
                        trackerSet.clear();

                        sharedData.setIpSet(new HashSet<String>());
                        sharedData.setTrackerSet(new HashSet<String>());
                        ipSet = sharedData.getIpSet();
                        ipSet.add(ipAddress);
                        sharedData.setDate(currentDate);
                        hitCounter++;
                        // fileName's extension is also configurable
                        // for simplicity - hardcoding it to ".ip"
                        fileName = currentDate.toString() + ".ip";
                    }
                    sharedData.setHitCounter(hitCounter);
                    currentHitCounter = hitCounter;
                }
                writer.println(currentHitCounter);
                socket.close();
                
                // file handling operations starts here
                if (isDumpRequired) {
                    FileHandler.updateFiles(fileName, currentHitCounter, tempIpSet);
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
        Server server = new Server(serverConfigurations.getPort());
    }
}