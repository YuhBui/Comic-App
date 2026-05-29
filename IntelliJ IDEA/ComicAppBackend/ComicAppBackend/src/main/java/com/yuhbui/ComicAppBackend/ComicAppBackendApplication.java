package com.yuhbui.ComicAppBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class ComicAppBackendApplication {

	public static void main(String[] args) {

		SpringApplication.run(ComicAppBackendApplication.class, args);
	}

	@Configuration
	public class WebConfig implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/uploads/**")
					.addResourceLocations("file:uploads/")
					.setCachePeriod(0); // THÊM DÒNG NÀY: Tắt hoàn toàn bộ nhớ đệm của Spring Boot
		}
	}
}

