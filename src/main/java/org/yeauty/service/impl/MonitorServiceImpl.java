package org.yeauty.service.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yeauty.pojo.DiscoverConfigBO;
import org.yeauty.service.MonitorService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonitorServiceImpl implements MonitorService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorServiceImpl.class);

    private static final String DEFAULT_SERVER = "127.0.0.1:65535";

    private AtomicLong lastReloadTime = new AtomicLong(0);

    @Override
    public void updateNginxFromNacos(File configFile) throws IOException, InterruptedException, NacosException {
        Properties pro = new Properties();
        FileInputStream in = new FileInputStream(configFile);
        pro.load(in);
        in.close();

        //判断nginx的指令是否可用
        String cmd = pro.getProperty(NGINX_CMD);
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
        String nacosAddr = pro.getProperty(NACOS_ADDR);
        if (StringUtils.isEmpty(nacosAddr)) {
            throw new IllegalArgumentException(NACOS_ADDR + " is empty");
        }
        NamingService namingService = NacosFactory.createNamingService(nacosAddr);

        //获取配置项
        List<DiscoverConfigBO> list = new ArrayList<>();
        int num = 0;
        while (true) {
            String configPath = pro.getProperty(NGINX_CONFIG + "." + num);
            String proxyPass = pro.getProperty(NGINX_PROXY_PASS + "." + num);
            String serviceName = pro.getProperty(NACOS_SERVICE_NAME + "." + num);
            if (StringUtils.isEmpty(configPath) || StringUtils.isEmpty(proxyPass) || StringUtils.isEmpty(serviceName)) {
                break;
            }
            DiscoverConfigBO discoverConfigBO = new DiscoverConfigBO(configPath, proxyPass, serviceName);
            list.add(discoverConfigBO);
            num++;
        }
        if (list.size() == 0) {
            throw new IllegalArgumentException(NGINX_CONFIG + "," + NGINX_PROXY_PASS + "," + NACOS_SERVICE_NAME + " are at least one group exists ");
        }

        //开始监听nacos
        for (DiscoverConfigBO configBO : list) {
            namingService.subscribe(configBO.getServiceName(),
                    event -> {
                        try {
                            List<Instance> instances = namingService.getAllInstances(configBO.getServiceName());
                            //更新nginx中的proxy_pass
                            refreshProxyPass(instances, configBO.getProxyPass(), configBO.getConfigPath());
                            logger.info("proxy_pass:{} update success!", configBO.getServiceName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
        }

        //开启线程定时reload
        new Thread(() -> {
            String intervalStr = pro.getProperty(RELOAD_INTERVAL, "0");
            long interval = 1000;
            try {
                interval = Long.parseLong(intervalStr);
            } catch (NumberFormatException e) {
                logger.warn("incorrect parameter :{} ", RELOAD_INTERVAL);
            }
            while (true) {
                if (lastReloadTime.get() == 0L || System.currentTimeMillis() - lastReloadTime.get() < interval) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(interval);
                        continue;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    //尝试nginx -t ,查看是否有语法错误 0正确 1错误
                    Process process = Runtime.getRuntime().exec(cmd + " -t");
                    int result = process.waitFor();
                    if (result != 0) {
                        logger.error("nginx syntax incorrect , execute [{}] to get detail ", (cmd + " -t"));
                        return;
                    }
                    //nginx reload
                    process = Runtime.getRuntime().exec(cmd + " -s reload");
                    result = process.waitFor();
                    if (result != 0) {
                        logger.error("nginx reload incorrect , execute [{}] to get detail ", (cmd + " -s reload"));
                        return;
                    }
                    logger.info("nginx reload success!");
                } catch (Exception e) {
                    logger.error("reload nginx throw exception", e);
                }
            }
        }).start();

    }

    private void refreshProxyPass(List<Instance> instances, String nginxProxyPass, String nginxConfigPath) {
        //获取到proxy_pass对应的upstream
        Pattern pattern = Pattern.compile(UPSTREAM_REG.replace(PLACEHOLDER, nginxProxyPass));
        //判断文件是否存在
        File file = new File(nginxConfigPath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("file : " + nginxConfigPath + " is not exists or not a file");
        }
        Long length = file.length();
        byte[] bytes = new byte[length.intValue()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
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
            String newUpstream = UPSTREAM_FOMAT.replace(PLACEHOLDER, nginxProxyPass);
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

            //替换原有的upstream
            conf = matcher.replaceAll(newUpstream);
        } else {
            throw new IllegalArgumentException("can not found proxy_pass:" + nginxProxyPass);
        }
        try {
            FileWriter fileWriter = new FileWriter(file, false);
            fileWriter.write(conf);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
