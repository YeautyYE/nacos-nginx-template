# nacos-nginx-template
 [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)[![Gitter](https://travis-ci.org/alibaba/nacos.svg?branch=master)](https://travis-ci.org/alibaba/nacos)

-------

## 简介

本项目以Agent的形式让Nginx实现对Nacos的服务发现.

## 快速启动



1. #### 下载二进制包

   点击此处下载:[最新稳定版](https://github.com/YeautyYE/nacos-nginx-template/releases/)

2. #### 配置config.toml

   配置文件使用[TOML](<https://github.com/toml-lang/toml>)进行配置

   demo : {nacos-nginx-template.home}/conf/config.toml.example

| 参数               | 描述                                           | 例子                                                    |
| ------------------ | ---------------------------------------------- | ------------------------------------------------------- |
| nginx_cmd          | nginx命令的全路径                              | "/usr/sbin/nginx"                                       |
| nacos_addr         | nacos的地址                                    | "172.16.0.100:8848,172.16.0.101:8848,172.16.0.102:8848" |
| nacos_username     | nacos账号                                      | nacos没有开启鉴权则注解该字段                           |
| nacos_password     | nacos密码                                      | nacos没有开启鉴权则注解该字段                           |
| reload_interval    | nginx reload命令执行间隔时间（ms  默认值1000） | 1000                                                    |
| nacos_service_name | nacos服务名                                    | "com.nacos.service.impl.NacosService"                   |
| nginx_config       | 需要修改nginx配置的路径                        | "/etc/nginx/nginx.conf"                                 |
| nginx_upstream     | nginx中upstream的名字                          | "nacos-service"                                         |

3. #### 启动

```shell
sh bin/startup.sh
```

