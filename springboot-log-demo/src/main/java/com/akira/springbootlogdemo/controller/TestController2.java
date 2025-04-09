package com.akira.springbootlogdemo.controller;

import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController2 {

    // ========== GET 请求示例 ==========

    // 1. 基本GET请求 - 路径参数
    @GetMapping("/hello/{name}")
    public String helloPath(@PathVariable String name) {
        // /api/test/hello/张三
        return "Hello, " + name + "! (路径参数)";
    }

    // 2. GET请求 - 查询参数
    @GetMapping("/greet")
    public String greetQuery(@RequestParam String name,
                             @RequestParam(defaultValue = "18") int age) {
        // api/test/greet?name=李四&age=30
        return "Hello, " + name + "! You are " + age + " years old. (查询参数)";
    }

    // 3. GET请求 - 返回JSON
    @GetMapping("/user")
    public User getUser() {
        // /api/test/user
//        {
//            "name": "张三",
//                "email": "zhangsan@example.com",
//                "age": 25
//        }
        return new User("张三", "zhangsan@example.com", 25);
    }

    // 4. GET请求 - 请求头读取
//    @GetMapping("/header")
//    public String readHeader(@RequestHeader("User-Agent") String userAgent) {
//        return "Your User-Agent: " + userAgent;
//    }

    // ========== POST 请求示例 ==========

    // 5. POST请求 - 表单提交 (urlencoded)
    @PostMapping(path = "/form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String handleForm(@RequestParam String username,
                             @RequestParam String password) {
//        POST /api/test/form
//        Content-Type: application/x-www-form-urlencoded
//
//        username=admin&password=123456
        return "Received form: username=" + username + ", password=" + password;
    }

    // 6. POST请求 - JSON body
    @PostMapping(path = "/json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public User handleJson(@RequestBody User user) {
//        POST /api/test/json
//        Content-Type: application/json
//
//        {"name":"王五","email":"wangwu@test.com","age":28}
        user.setName("[Processed] " + user.getName());
        return user;
    }

    // 7. POST请求 - 多文件上传
    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String handleUpload(@RequestPart("file") MultipartFile file,
                               @RequestPart("meta") String meta) {
        // 使用 Postman 或 cURL 发送 multipart/form-data 请求
        return String.format("Received file: %s (%.2f KB), meta: %s",
                file.getOriginalFilename(),
                file.getSize() / 1024.0,
                meta);
    }

    // 8. POST请求 - 混合参数 (路径+查询+body)
    @PostMapping("/complex/{id}")
    public Map<String, Object> complexRequest(
            @PathVariable String id,
            @RequestParam String action,
            @RequestBody Map<String, Object> payload) {
//        POST /api/test/complex/1001?action=update
//        Content-Type: application/json
//
//        {"key1":"value1","key2":123}
        return Map.of(
                "id", id,
                "action", action,
                "payload", payload,
                "timestamp", System.currentTimeMillis()
        );
    }

    // ========== 内部类 ==========
    @Data
    static class User {
        private String name;
        private String email;
        private int age;

        // 构造方法、getter和setter
        public User(String name, String email, int age) {
            this.name = name;
            this.email = email;
            this.age = age;
        }

        // 必须有无参构造方法
        public User() {}

        // 省略getter和setter...
    }
}