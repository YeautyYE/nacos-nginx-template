package org.yeauty.pojo;

public class DiscoverConfigBO {

    private String configPath;
    private String upstream;
    private String serviceName;

    public DiscoverConfigBO(String configPath, String upstream, String serviceName) {
        this.configPath = configPath;
        this.upstream = upstream;
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return "DiscoverConfigBO{" +
                "configPath='" + configPath + '\'' +
                ", upstream='" + upstream + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getUpstream() {
        return upstream;
    }

    public void setUpstream(String upstream) {
        this.upstream = upstream;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
