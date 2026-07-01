package com.nextech.enterprisekbagent.controller;

import com.nextech.enterprisekbagent.agent.FactCheckAgent;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentDemoController {

    @Resource
    private FactCheckAgent factCheckAgent;

    @GetMapping("/fact-check")
    public String factCheck(@RequestParam(defaultValue = "员工远程办公需提前一天申请，审批通过后可使用公司设备办公。"
            + "远程办公期间需保证每日8小时在线，并参加每日站会。") String answer) {
        return factCheckAgent.execute(answer);
    }
}
