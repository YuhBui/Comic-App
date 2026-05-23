package com.yuhbui.ComicAppBackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public String testApi() {
        return "Kết nối Spring Boot thành công! Sẵn sàng code Android.";
    }
}
