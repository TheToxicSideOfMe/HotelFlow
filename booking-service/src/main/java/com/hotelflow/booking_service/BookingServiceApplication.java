package com.hotelflow.booking_service;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.hotelflow.booking_service.security.JwtForwardingInterceptor;

@SpringBootApplication
public class BookingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}
	
	@Bean
	public RestTemplate restTemplate(JwtForwardingInterceptor jwtInterceptor) {
	    RestTemplate restTemplate = new RestTemplate();
	    restTemplate.setInterceptors(List.of(jwtInterceptor));
	    return restTemplate;
	}

}
