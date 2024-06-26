package com.tilitili.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
	@Bean
	public CorsFilter corsFilter() {
		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		this.configPub(source);
		this.configDefault(source);
		return new CorsFilter(source);
	}

	private void configPub(UrlBasedCorsConfigurationSource source) {
		final CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.setAllowedOriginPatterns(Collections.singletonList("*"));
		config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept","Authorization", "Cache-Control"));
		config.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS"));

		source.registerCorsConfiguration("/api/pub/**", config);
	}

	private void configDefault(UrlBasedCorsConfigurationSource source) {
		final CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.setAllowedOriginPatterns(Collections.singletonList("*.tilitili.club"));
		config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept","Authorization", "Cache-Control"));
		config.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS"));

		source.registerCorsConfiguration("/**", config);
	}
}
