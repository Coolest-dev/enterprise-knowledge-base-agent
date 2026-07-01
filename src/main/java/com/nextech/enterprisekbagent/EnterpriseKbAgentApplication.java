package com.nextech.enterprisekbagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {
        // 默认排除数据源自动配置；使用 PgVector 时从 exclude 中移除 DataSourceAutoConfiguration.class
        DataSourceAutoConfiguration.class
})
public class EnterpriseKbAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseKbAgentApplication.class, args);
    }

}
