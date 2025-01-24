package com.museum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class MvcConfig extends WebMvcConfigurationSupport {

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/userClient/**")
                .addResourceLocations("classpath:/userClient/");

        registry.addResourceHandler("/dist/**")
                .addResourceLocations("classpath:/dist/");
    }
}
