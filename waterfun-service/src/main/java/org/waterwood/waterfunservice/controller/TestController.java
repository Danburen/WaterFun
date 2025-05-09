package org.waterwood.waterfunservice.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // 基础GET请求
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    // 带路径参数的GET请求
    @GetMapping("/user/{id}")
    public String getUserById(@PathVariable Long id) {
        return "User ID: " + id;
    }

    // 带查询参数的GET请求
    @GetMapping("/search")
    public String search(@RequestParam String keyword) {
        return "Search Keyword: " + keyword;
    }

    // 返回JSON数据
    @GetMapping("/data")
    public TestData getData() {
        return new TestData(1, "Test Object");
    }

    // 内部类（或单独定义DTO）
    static class TestData {
        private int id;
        private String name;

        // 构造方法、Getter/Setter（Lombok可省略）
        public TestData(int id, String name) {
            this.id = id;
            this.name = name;
        }

        // 如果不用Lombok，需手动添加Getter
        public int getId() { return id; }
        public String getName() { return name; }
    }
}