package com.dc.learning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = "com.dc.learning.mapper")
@EnableScheduling
public class DcApplication {
	public static void main(String[] args) {
		SpringApplication.run(DcApplication.class, args);
	}
}
