package com.ety.natively;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan(basePackages = "com.ety.natively.mapper")
@EnableScheduling
@EnableTransactionManagement
@EnableAsync
public class NativelyApplication {
	public static void main(String[] args) {
		SpringApplication.run(NativelyApplication.class, args);
	}
}
