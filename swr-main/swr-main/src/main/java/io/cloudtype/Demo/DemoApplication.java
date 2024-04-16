package io.cloudtype.Demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DemoApplication {
	public static void main(String[] args) {
		// 시스템 시간대를 KST로 설정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

		SpringApplication.run(DemoApplication.class, args);
	}
}
