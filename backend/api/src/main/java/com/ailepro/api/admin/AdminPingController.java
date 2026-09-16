package com.ailepro.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
public class AdminPingController {
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("ok", "true");
    }
}
