package org.yeauty;

import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yeauty.service.MonitorService;
import org.yeauty.service.StartupService;
import org.yeauty.service.impl.MonitorServiceImpl;
import org.yeauty.service.impl.StartupServiceImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    static String PLACEHOLDER = "@{placeholder}";
    static String PLACEHOLDER_SERVER = "@{placeholder_server}";
    static String UPSTREAM_REG = "upstream\\s*" + PLACEHOLDER + "\\s*\\{[^}]+\\}";
    static String UPSTREAM_FOMAT = "upstream " + PLACEHOLDER + " {\n " + PLACEHOLDER_SERVER + " \n}";

    public static void main(String[] args) throws IOException, InterruptedException, NacosException {

        //获取config路径
        StartupService startupService = new StartupServiceImpl();
        Map<String, String> map = startupService.argsToMap(args);
        String configPath = map.get("config");
        if (configPath == null || "".equals(configPath.trim())) {
            throw new IllegalArgumentException("config is empty");
        }
        //判断config是否存在
        File file = new File(configPath);
        if (!file.exists()) {
            throw new FileNotFoundException("config not found");
        }
        //判断是否为文件
        if (!file.isFile()) {
            throw new FileNotFoundException("config is not a file");
        }

        //开始进行监听
        MonitorService monitorService = new MonitorServiceImpl();
        monitorService.updateNginxFromNacos(file);

        System.out.println("nacos-nginx-template start up!");

        new CountDownLatch(1).await();
    }


}
