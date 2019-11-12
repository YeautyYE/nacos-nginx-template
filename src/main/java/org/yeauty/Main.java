package org.yeauty;

import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.yeauty.service.MonitorService;
import org.yeauty.service.StartupService;
import org.yeauty.service.impl.MonitorServiceImpl;
import org.yeauty.service.impl.StartupServiceImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

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

        Path source = Paths.get(configPath);
        TomlParseResult result = Toml.parse(source);
        if (result.hasErrors()) {
            result.errors().forEach(error -> System.err.println(error.toString()));
            throw new InterruptedException();
        }

        //开始进行监听
        MonitorService monitorService = new MonitorServiceImpl();
        monitorService.updateNginxFromNacos(result);

        logger.info("nacos-nginx-template start up!");

        new CountDownLatch(1).await();
    }


}
