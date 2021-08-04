package org.yeauty.service.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.TomlInvalidTypeException;
import org.tomlj.TomlParseResult;
import org.yeauty.pojo.DiscoverConfigBO;
import org.yeauty.service.MonitorService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonitorServiceImpl implements MonitorService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorServiceImpl.class);

    private static final String DEFAULT_SERVER = "127.0.0.1:65535";

    private AtomicLong lastReloadTime = new AtomicLong(0);

    @Override
    public void updateNginxFromNacos(TomlParseResult tomlParseResult) throws IOException, InterruptedException, NacosException {

        boolean containNginxCmd = tomlParseResult.contains(NGINX_CMD);
        boolean containNacosAddr = tomlParseResult.contains(NACOS_ADDR);
        if (!containNginxCmd) {
            throw new IllegalArgumentException(NGINX_CMD + " is no such");
        }
        if (!containNacosAddr) {
            throw new IllegalArgumentException(NACOS_ADDR + " is no such");
        }

        //判断nginx的指令是否可用
        String cmd = tomlParseResult.getString(NGINX_CMD);
        if (StringUtils.isEmpty(cmd)) {
            throw new IllegalArgumentException(NGINX_CMD + " is empty");
        }
        int i = -1;
        try {
            Process process = Runtime.getRuntime().exec(cmd + " -V");
            i = process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException(NGINX_CMD + " is incorrect");
        }
        if (i != 0) {
            throw new IllegalArgumentException(NGINX_CMD + " is incorrect");
        }

        //判断nacos地址
        String nacosAddr = tomlParseResult.getString(NACOS_ADDR);
        String nacosUsername = tomlParseResult.getString(NACOS_USERNAME);
        String nacosPassword = tomlParseResult.getString(NACOS_PASSWORD);
        if (StringUtils.isEmpty(nacosAddr)) {
            throw new IllegalArgumentException(NACOS_ADDR + " is empty");
        }
        Properties properties = new Properties();
        properties.put("serverAddr", nacosAddr);

        if (StringUtils.isNotEmpty(nacosUsername) && StringUtils.isNotEmpty(nacosPassword)) {
            properties.put("username", nacosUsername);
            properties.put("password", nacosPassword);
        }

        NamingService namingService = NacosFactory.createNamingService(properties);
        logger.info("server status : {}",namingService.getServerStatus());

        //获取配置项
        List<DiscoverConfigBO> list = new ArrayList<>();
        int num = 0;
        Set<String> groupNames = tomlParseResult.keySet();
        groupNames.stream()
                .filter(groupName -> !groupName.equals(NGINX_CMD)
                        && !groupName.equals(NACOS_ADDR)
                        && !groupName.equals(NACOS_USERNAME)
                        && !groupName.equals(NACOS_PASSWORD)
                        && !groupName.equals(RELOAD_INTERVAL))
                .forEach(groupName -> {
                    String configPath = tomlParseResult.getString(groupName + "." + NGINX_CONFIG);
                    String upstream = tomlParseResult.getString(groupName + "." + NGINX_UPSTREAM);
                    String serviceName = tomlParseResult.getString(groupName + "." + NACOS_SERVICE_NAME);
                    if (StringUtils.isEmpty(configPath) || StringUtils.isEmpty(upstream) || StringUtils.isEmpty(serviceName)) {
                        logger.warn("group_name:{} . {} or {} or {} is empty", groupName, NGINX_CONFIG, NGINX_UPSTREAM, NACOS_SERVICE_NAME);
                        return;
                    }
                    DiscoverConfigBO discoverConfigBO = new DiscoverConfigBO(configPath, upstream, serviceName);
                    list.add(discoverConfigBO);
                    logger.info("add config success , group_name:{} ", groupName);
                });

        if (list.size() == 0) {
            throw new IllegalArgumentException(NGINX_CONFIG + "," + NGINX_UPSTREAM + "," + NACOS_SERVICE_NAME + " are at least one group exists ");
        }

        //开始监听nacos
        for (DiscoverConfigBO configBO : list) {
            namingService.subscribe(configBO.getServiceName(),
                    event -> {
                        try {
                            List<Instance> instances = namingService.getAllInstances(configBO.getServiceName());
                            //更新nginx中的upstream
                            boolean updated = refreshUpstream(instances, configBO.getUpstream(), configBO.getConfigPath());
                            if (updated) {
                                lastReloadTime.set(System.currentTimeMillis());
                                logger.info("upstream:{} update success!", configBO.getServiceName());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
        }

        //开启线程定时reload
        new Thread(() -> {
            long interval = 1000;
            if (tomlParseResult.contains(RELOAD_INTERVAL)) {
                try {
                    interval = tomlParseResult.getLong(RELOAD_INTERVAL);
                } catch (TomlInvalidTypeException e) {
                    logger.warn("incorrect parameter :{} ", RELOAD_INTERVAL);
                }
            }
            Process process = null;
            boolean result = false;
            while (true) {
                if (lastReloadTime.get() == 0L || (System.currentTimeMillis() - lastReloadTime.get()) < interval) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(interval);
                        continue;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    //尝试nginx -t ,查看是否有语法错误 0正确 1错误
                    process = Runtime.getRuntime().exec(cmd + " -t");
                    result = process.waitFor(10, TimeUnit.SECONDS);
                    if (!result) {
                        logger.error("nginx timeout , execute [{}] to get detail ", (cmd + " -t"));
                        continue;
                    }
                    if (process.exitValue() != 0) {
                        logger.error("nginx syntax incorrect , execute [{}] to get detail ", (cmd + " -t"));
                        continue;
                    }
                    //nginx reload
                    process = Runtime.getRuntime().exec(cmd + " -s reload");
                    result = process.waitFor(10, TimeUnit.SECONDS);
                    if (!result) {
                        logger.error("nginx timeout , execute [{}] to get detail ", (cmd + " -t"));
                        continue;
                    }
                    if (process.exitValue() != 0) {
                        logger.error("nginx reload incorrect , execute [{}] to get detail ", (cmd + " -s reload"));
                        continue;
                    }
                    lastReloadTime.set(0L);
                    logger.info("nginx reload success!");
                } catch (Exception e) {
                    logger.error("reload nginx throw exception", e);
                }
            }
        }, "reload-nginx").start();

    }

    private boolean refreshUpstream(List<Instance> instances, String nginxUpstream, String nginxConfigPath) {
        //获取到upstream
        Pattern pattern = Pattern.compile(UPSTREAM_REG.replace(PLACEHOLDER, nginxUpstream));
        //判断文件是否存在
        File file = new File(nginxConfigPath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("file : " + nginxConfigPath + " is not exists or not a file");
        }
        Long length = file.length();
        byte[] bytes = new byte[length.intValue()];
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //获取到配置文件内容
        String conf = new String(bytes);
        //匹配对应的upstream
        Matcher matcher = pattern.matcher(conf);
        if (matcher.find()) {
            String formatSymbol = "";
            String oldUpstream = matcher.group();
            //计算出旧的upstream到左边的距离
            int index = conf.indexOf(oldUpstream);
            while (index != 0 && (conf.charAt(index - 1) == ' ' || conf.charAt(index - 1) == '\t')) {
                formatSymbol += conf.charAt(index - 1);
                index--;
            }

            //拼接新的upstream
            String newUpstream = UPSTREAM_FOMAT.replace(PLACEHOLDER, nginxUpstream);
            StringBuffer servers = new StringBuffer();
            if (instances.size() > 0) {
                for (Instance instance : instances) {
                    //不健康或不可用的跳过
                    if (!instance.isHealthy() || !instance.isEnabled()) {
                        continue;
                    }
                    String ip = instance.getIp();
                    int port = instance.getPort();
                    servers.append(formatSymbol + "    server " + ip + ":" + port + ";\n");
                }
            }
            if (servers.length() == 0) {
                //如果没有对应的服务，使用默认的服务防止nginx报错
                servers.append(formatSymbol + "    server " + DEFAULT_SERVER + ";\n");
            }
            servers.append(formatSymbol);
            newUpstream = newUpstream.replace(PLACEHOLDER_SERVER, servers.toString());
            if (oldUpstream.equals(newUpstream)) {
                return false;
            }
            //替换原有的upstream
            conf = matcher.replaceAll(newUpstream);
        } else {
            throw new IllegalArgumentException("can not found upstream:" + nginxUpstream);
        }
        try (FileWriter fileWriter = new FileWriter(file, false)) {
            fileWriter.write(conf);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}
