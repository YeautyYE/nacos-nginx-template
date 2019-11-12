package org.yeauty.service;

import com.alibaba.nacos.api.exception.NacosException;
import org.tomlj.TomlParseResult;

import java.io.IOException;

public interface MonitorService {

    String PLACEHOLDER = "@{placeholder}";
    String PLACEHOLDER_SERVER = "@{placeholder_server}";
    String UPSTREAM_REG = "upstream\\s*" + PLACEHOLDER + "\\s*\\{[^}]+\\}";
    String UPSTREAM_FOMAT = "upstream " + PLACEHOLDER + " {\n" + PLACEHOLDER_SERVER + "}";

    String NGINX_CMD = "nginx_cmd";
    String NACOS_ADDR = "nacos_addr";

    String NGINX_CONFIG = "nginx_config";
    String NGINX_UPSTREAM = "nginx_upstream";
    String NACOS_SERVICE_NAME = "nacos_service_name";
    String RELOAD_INTERVAL = "reload_interval";

    void updateNginxFromNacos(TomlParseResult result) throws IOException, InterruptedException, NacosException;
}
