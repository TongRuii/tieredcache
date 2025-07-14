package com.cache.plugin.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * TieredCache 演示应用
 * 
 * 这个示例展示了如何在真实的Spring Boot应用中使用TieredCache
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.cache.plugin"})
public class DemoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        
        System.out.println("\n=== TieredCache 演示应用已启动 ===");
        System.out.println("访问 http://localhost:8080 查看演示页面");
        System.out.println("或运行 DemoRunner 查看控制台演示");
    }
}