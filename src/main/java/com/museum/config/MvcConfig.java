package com.museum.config;

import com.museum.utils.LoginInterceptor;
import com.museum.utils.RefreshTokenInterceptor;
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
                        "/collect/getdataList",
                        "/announcement/listMsAnnouncement",
                        "/announcement/listMsAnnouncementTop",
                        "/reserve/listMsReserveClient",
                        "/feedBack/listFeedBackByUser",
                        "/ai/**",
                        "/file/**",
                        "/dic/listColType",
                        "/userClient/**",
                        "/admin/**",
                        "/news/**"

                ).order(1);
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**").order(0);
    }



}
