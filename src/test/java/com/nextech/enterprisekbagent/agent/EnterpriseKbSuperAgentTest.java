package com.nextech.enterprisekbagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EnterpriseKbSuperAgentTest {

    @Resource
    private EnterpriseKbSuperAgent enterpriseKbSuperAgent;

    @Test
    void run() {
        String userPrompt = "帮我搜索 NexTech Solutions 生产环境 P0 故障应急响应流程";
        String answer = enterpriseKbSuperAgent.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}
