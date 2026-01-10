package org.example.drawsystemserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.drawsystemserver.mapper")
public class DrawSystemServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrawSystemServerApplication.class, args);
    }
}
