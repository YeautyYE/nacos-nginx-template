# nacos-nginx-template
 [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)[![Gitter](https://travis-ci.org/alibaba/nacos.svg?branch=master)](https://travis-ci.org/alibaba/nacos)

-------

## 简介

本项目以Agent的形式让Nginx实现对Nacos的服务发现.

## 快速启动



1. #### 下载二进制包

   点击此处下载:[最新稳定版](https://github.com/YeautyYE/nacos-nginx-template/releases/)

2. #### 配置config.properties

| 参数                     | 描述                                           | 例子                                                  |
| ------------------------ | ---------------------------------------------- | ----------------------------------------------------- |
| nginx.cmd                | nginx命令的全路径                              | /usr/sbin/nginx                                       |
| nacos.addr               | nacos的地址                                    | 172.16.0.100:8848,172.16.0.101:8848,172.16.0.102:8848 |
| reload-interval          | nginx reload命令执行间隔时间（ms  默认值1000） | 1000                                                  |
| nacos.service-name.{num} | nacos服务名; {num}从0开始递增                  | com.nacos.service.impl.NacosService                   |
| nginx.config.{num}       | 需要修改nginx配置的路径;{num}从0开始递增       | /etc/nginx/nginx.conf                                 |
| nginx.proxy-pass.{num}   | nginx中proxy_pass的名字;{num}从0开始递增       | nacos-service                                         |

3. #### 启动

```shell
sh bin/startup.sh
```

