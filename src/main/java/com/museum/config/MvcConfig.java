package com.museum.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private AsyncTaskExecutor taskExecutor;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(30000)
                .setTaskExecutor(taskExecutor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/userClient/**")
                .addResourceLocations("classpath:/userClient/");

        registry.addResourceHandler("/dist/**")
                .addResourceLocations("classpath:/dist/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/register",
                        "/user/login",
                        "/collect/getdata",
                        "/collect/getdataTop",
                        "/announcement/listMsAnnouncement",
                        "/announcement/listMsAnnouncementTop",
                        "/ai/**",
                        "/file/**",
                        "/dic/listDicByTyp",
                        "/userClient/**"
                );
    }



}
