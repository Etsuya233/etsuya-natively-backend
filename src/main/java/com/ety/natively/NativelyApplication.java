package com.ety.natively;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = "com.ety.natively.mapper")
@EnableScheduling
public class NativelyApplication {
	public static void main(String[] args) {
		SpringApplication.run(NativelyApplication.class, args);
	}
}
