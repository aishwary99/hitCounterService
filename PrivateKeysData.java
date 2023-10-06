import java.util.List;

public class PrivateKeysData {
    private List<PrivateKeyInfo> privateKeys;

    public PrivateKeysData() {
    }

    public PrivateKeysData(List<PrivateKeyInfo> privateKeys) {
        this.privateKeys = privateKeys;
    }

    public List<PrivateKeyInfo> getPrivateKeys() {
        return privateKeys;
    }

    public void setPrivateKeys(List<PrivateKeyInfo> privateKeys) {
        this.privateKeys = privateKeys;
    }

    public String toString() {
        return "PrivateKeysData{" +
                "privateKeys=" + privateKeys +
                '}';
    }
}

class PrivateKeyInfo {
    private String privateKey;
    private List<String> listedIps;

    public PrivateKeyInfo() {
    }

    public PrivateKeyInfo(String privateKey, List<String> listedIps) {
        this.privateKey = privateKey;
        this.listedIps = listedIps;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public List<String> getListedIps() {
        return listedIps;
    }

    public void setListedIps(List<String> listedIps) {
        this.listedIps = listedIps;
    }

    public String toString() {
        return "PrivateKeyInfo{" +
                "privateKey='" + privateKey + '\'' +
                ", listedIps=" + listedIps +
                '}';
    }
}