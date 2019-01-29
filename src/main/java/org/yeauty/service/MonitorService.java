package org.yeauty.service;

import com.alibaba.nacos.api.exception.NacosException;

import java.io.File;
import java.io.IOException;

public interface MonitorService {

    String PLACEHOLDER = "@{placeholder}";
    String PLACEHOLDER_SERVER = "@{placeholder_server}";
    String UPSTREAM_REG = "upstream\\s*" + PLACEHOLDER + "\\s*\\{[^}]+\\}";
    String UPSTREAM_FOMAT = "upstream " + PLACEHOLDER + " {\n" + PLACEHOLDER_SERVER + "}";

    String NGINX_CMD = "nginx.cmd";
    String NGINX_CONFIG = "nginx.config";
    String NGINX_PROXY_PASS = "nginx.proxy-pass";
    String NACOS_ADDR = "nacos.addr";
    String NACOS_SERVICE_NAME = "nacos.service-name";

    void updateNginxFromNacos(File configFile) throws IOException, InterruptedException, NacosException;
}
