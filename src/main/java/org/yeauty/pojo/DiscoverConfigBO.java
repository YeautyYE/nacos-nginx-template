package org.yeauty.pojo;

public class DiscoverConfigBO {

    private String configPath;
    private String proxyPass;
    private String serviceName;

    public DiscoverConfigBO(String configPath, String proxyPass, String serviceName) {
        this.configPath = configPath;
        this.proxyPass = proxyPass;
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return "DiscoverConfigBO{" +
                "configPath='" + configPath + '\'' +
                ", proxyPass='" + proxyPass + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getProxyPass() {
        return proxyPass;
    }

    public void setProxyPass(String proxyPass) {
        this.proxyPass = proxyPass;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
