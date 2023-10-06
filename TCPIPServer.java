import java.io.*;
import java.net.*;
import java.util.*;
import java.time.*;
import com.google.gson.*;

class SharedData {
    private Map<String, Set<String>> privateKeyToWhitelistedIpsMap;
    private Map<String, Set<String>> privateKeyToIpsMap;
    private Map<String, Integer> privateKeyToHitCounterMap;
    private Map<String, Set<String>> privateKeyToTrackerSetMap;
    private LocalDate date;
    private String hitCounterFile;

    public SharedData(String hitCounterFile, Map<String, Set<String>> privateKeyToWhitelistedIpsMap) {
        this.date = LocalDate.now();
        this.hitCounterFile = hitCounterFile;
        this.privateKeyToWhitelistedIpsMap = privateKeyToWhitelistedIpsMap;
        initializeMaps();
        loadData();
    }

    public void setIpSet(String privateKey, Set<String> ipSet) {
        this.privateKeyToIpsMap.put(privateKey, ipSet);
    }

    public Set<String> getIpSet(String privateKey) {
        return this.privateKeyToIpsMap.get(privateKey);
    }

    public void setTrackerSet(String privateKey, Set<String> trackerSet) {
        this.privateKeyToTrackerSetMap.put(privateKey, trackerSet);
    }

    public Set<String> getTrackerSet(String privateKey) {
        return this.privateKeyToTrackerSetMap.get(privateKey);
    }

    public void setHitCounter(String privateKey, int hitCounter) {
        this.privateKeyToHitCounterMap.put(privateKey, hitCounter);
    }

    public int getHitCounter(String privateKey) {
        return this.privateKeyToHitCounterMap.get(privateKey);
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public boolean isIpWhitelisted(String privateKey, String ipAddress) {
        return this.privateKeyToWhitelistedIpsMap.get(privateKey).contains(ipAddress);
    }

    private void initializeMaps() {
        this.privateKeyToIpsMap = new HashMap<>(this.privateKeyToWhitelistedIpsMap.size());
        this.privateKeyToHitCounterMap = new HashMap<>(this.privateKeyToWhitelistedIpsMap.size());
        this.privateKeyToTrackerSetMap = new HashMap<>(this.privateKeyToWhitelistedIpsMap.size());

        for (String privateKey : this.privateKeyToWhitelistedIpsMap.keySet()) {
            this.privateKeyToIpsMap.put(privateKey, new HashSet<String>());
            this.privateKeyToHitCounterMap.put(privateKey, 0);
            this.privateKeyToTrackerSetMap.put(privateKey, new HashSet<String>());
        }
    }

    /**
     * Loads the persisted data of ip's and hitCounter in sharedData
     * Scenario - B.E Server Failover / Restarts
     * */
    private void loadData() {
        String directory = LocalDate.now().toString();

        for (Map.Entry<String, Set<String>> entry : this.privateKeyToWhitelistedIpsMap.entrySet()) {
            String privateKey = entry.getKey();
            String ipFileName = directory + "/" + privateKey + ".ip";
            String hitCounterFileName = directory + "/" + privateKey + "." + hitCounterFile;

            try {
                File file = new File(ipFileName);
                if (file.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String ip = reader.readLine();
                        if (ip == null) break;
                        this.privateKeyToIpsMap.get(privateKey).add(ip);
                    }
                    reader.close();
                }

                file = new File(hitCounterFileName);
                if (file.exists()) {
                    FileReader fileReader = new FileReader(file);
                    StringBuilder hitCounterBuilder = new StringBuilder();
                    while (true) {
                        int hitCounterRead = fileReader.read();
                        if (hitCounterRead == -1) break;
                        hitCounterBuilder.append((char) hitCounterRead);
                    }

                    this.privateKeyToHitCounterMap.put(privateKey, Integer.parseInt(hitCounterBuilder.toString().trim()));
                    fileReader.close();
                }
            } catch (Exception e) {}
        }
    }
}

class FileHandler {
    public synchronized static void updateFiles(String ipFileName, String directoryPath ,int hitCounter, Set<String> ipSet, String hitCounterFile) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists()) directory.mkdir();

            ipFileName = directoryPath + "/" + ipFileName;
            File file = new File(ipFileName);
            if (!file.exists()) file.createNewFile();
            FileWriter fileWriter = new FileWriter(file, true);
            for (String ip : ipSet) {
                fileWriter.write(ip + "\n");
            }
            fileWriter.close();

            file = new File(directoryPath + "/" + hitCounterFile);
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
    private String hitCounterFile;
    private int hitCounterFileThreshold;
    private static SharedData sharedData;

    public Server(int port, String hitCounterFile, int hitCounterFileThreshold, Map<String, Set<String>> privateKeyToIpsMap) {
        this.port = port;
        this.hitCounterFile = hitCounterFile;
        this.hitCounterFileThreshold = hitCounterFileThreshold;
        this.sharedData = new SharedData(this.hitCounterFile, privateKeyToIpsMap);
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
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                String request = reader.readLine();

                String ipAddress = "";
                String privateKey = "";
                if (request.indexOf(",") != -1) {
                    String[] splittedRequest = request.split(",");
                    privateKey = splittedRequest[0].trim();
                    ipAddress = splittedRequest[1].trim();
                } else {
                    writer.println("");
                    socket.close();
                    return;
                }
                         
                boolean isDumpRequired = false;
                String fileName = "";
                String directoryPath = "";
                String hitCounterFilePath = "";
                Set<String> tempIpSet = null;
                int currentHitCounter = 0;

                synchronized(sharedData) {
                    int hitCounter = sharedData.getHitCounter(privateKey);
                    if (!sharedData.isIpWhitelisted(privateKey, ipAddress)) {
                        writer.println(hitCounter);
                        socket.close();
                        return;
                    }

                    Set<String> ipSet = sharedData.getIpSet(privateKey);
                    Set<String> trackerSet = sharedData.getTrackerSet(privateKey);

                    directoryPath = sharedData.getDate().toString();
                    fileName = privateKey + ".ip";
                    hitCounterFilePath = privateKey + "." + hitCounterFile;

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
                    }else {
                        isDumpRequired = true;
                        tempIpSet = new HashSet<>(trackerSet);
                        trackerSet.clear();

                        sharedData.setIpSet(privateKey, new HashSet<String>());
                        sharedData.setTrackerSet(privateKey, new HashSet<String>());
                        ipSet = sharedData.getIpSet(privateKey);
                        ipSet.add(ipAddress);
                        sharedData.setDate(currentDate);
                        hitCounter++;

                        directoryPath = sharedData.getDate().toString();
                        fileName = privateKey + ".ip";
                        hitCounterFilePath = privateKey + "." + hitCounterFile;
                    }
                    sharedData.setHitCounter(privateKey, hitCounter);
                    currentHitCounter = hitCounter;
                }
                writer.println(currentHitCounter);
                socket.close();

                if (isDumpRequired) {
                    FileHandler.updateFiles(fileName, directoryPath, currentHitCounter, tempIpSet, hitCounterFilePath);
                    tempIpSet.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

public class TCPIPServer {
    private static Map<String, Set<String>> getPrivateKeys() {
        String privateKeysFilePath = "private-keys/privateKeys.json";
        PrivateKeysData privateKeysData = null;

        try {
            FileReader reader = new FileReader(privateKeysFilePath);
            Gson gson = new Gson();
            privateKeysData = gson.fromJson(reader, PrivateKeysData.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Set<String>> privateKeyToIpsMap = new HashMap<>();
        if (privateKeysData != null) {
            for (PrivateKeyInfo privateKeyInfo : privateKeysData.getPrivateKeys()) {
                String privateKey = privateKeyInfo.getPrivateKey().trim();
                Set<String> listedIpsSet = new HashSet();
                for (String listedIp : privateKeyInfo.getListedIps()) {
                    listedIpsSet.add(listedIp.trim());
                }
                privateKeyToIpsMap.put(privateKey, listedIpsSet);
            }
        }

        return privateKeyToIpsMap;
    }

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
        Map<String, Set<String>> privateKeyToIpsMap = getPrivateKeys();
        Server server = new Server(serverConfigurations.getPort(), serverConfigurations.getHitCounterFilePath(), serverConfigurations.getHitCounterFileThreshold(), privateKeyToIpsMap);
    }
}