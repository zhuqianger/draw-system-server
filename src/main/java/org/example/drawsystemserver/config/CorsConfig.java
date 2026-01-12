package org.example.drawsystemserver.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许携带凭证（cookies、authorization headers等）
        config.setAllowCredentials(true);
        
        // 允许所有来源（使用OriginPattern支持通配符，兼容allowCredentials）
        config.addAllowedOriginPattern("*");
        
        // 允许所有请求头
        config.addAllowedHeader("*");
        
        // 允许所有HTTP方法
        config.addAllowedMethod("*");
        
        // 设置预检请求的缓存时间（秒）
        config.setMaxAge(3600L);
        
        // 暴露响应头，允许前端访问
        config.addExposedHeader("*");
        
        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", config);
        
        // 创建FilterRegistrationBean，设置最高优先级
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 设置最高优先级，确保在其他过滤器之前执行
        return bean;
    }
}
