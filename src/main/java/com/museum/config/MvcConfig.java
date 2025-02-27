package com.museum.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.*;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private AsyncTaskExecutor taskExecutor;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
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
