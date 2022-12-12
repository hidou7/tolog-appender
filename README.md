# 简介

tolog-appender是基于logback的appender日志插件，可以将日志信息直接输出到数据库。适用于中小型架构、又不想引入重量级的ELK日志系统。

# 使用

## 数据库表

在script包下，暂时仅支持Mysql。

## maven

```xml
<dependencies>
  <dependency>
    <groupId>io.github.hidou7</groupId>
    <artifactId>tolog-appender</artifactId>
    <version>1.0.2</version>
  </dependency>
</dependencies>

<!-- maven仓库 -->
<repositories>
    <repository>
        <id>ali-repo</id>
        <name>ali-repo</name>
        <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
        <layout>default</layout>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
    <repository>
        <id>maven-repo</id>
        <name>maven-repo</name>
        <url>https://repo1.maven.org/maven2/</url>
        <layout>default</layout>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```

## logback配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration debug="false">

    <property name="log.pattern" value="%d{yyyy-MM-hh HH:mm:ss.SSS} %-5level --- [%thread] %logger{20}.%method: %msg%n"/>

    <springProperty scope="context" name="driverClassName" source="logback.dbappender.driverClassName"/>
    <springProperty scope="context" name="jdbcUrl" source="logback.dbappender.jdbcUrl"/>
    <springProperty scope="context" name="username" source="logback.dbappender.username"/>
    <springProperty scope="context" name="password" source="logback.dbappender.password"/>

    <!-- 输出到控制台 -->
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${log.pattern}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 输出到数据库 -->
    <appender name="db" class="io.github.hidou7.tolog.DBAppender">
        <connectionSource class="ch.qos.logback.core.db.DataSourceConnectionSource">
            <dataSource class="com.zaxxer.hikari.HikariDataSource">
                <!--不同连接池的以下标签可能是不同的，如DruidDataSource的是url不是jdbcUrl-->
                <driverClassName>${driverClassName}</driverClassName>
                <jdbcUrl>${jdbcUrl}</jdbcUrl>
                <username>${username}</username>
                <password>${password}</password>
            </dataSource>
        </connectionSource>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
    </appender>

    <root level="info">
        <appender-ref ref="console" />
        <appender-ref ref="db" />
    </root>
</configuration>
```

## application.xml

```
logback:
  dbappender:
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/tolog?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&allowMultiQueries=true
    username: username
    password: password
```

