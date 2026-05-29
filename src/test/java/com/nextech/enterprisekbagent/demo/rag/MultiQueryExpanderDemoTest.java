package com.nextech.enterprisekbagent.demo.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MultiQueryExpanderDemoTest {

    @Resource
    private MultiQueryExpanderDemo multiQueryExpanderDemo;

    @Test
    void expand() {
        multiQueryExpanderDemo.expand("企业远程办公政策是什么？请详细说明");
    }
}
