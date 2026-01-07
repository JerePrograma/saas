package com.scalaris.bootstrap.config;

import com.scalaris.shared.tenancy.TenantContextFilter;
import com.scalaris.shared.tenancy.TenantResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFiltersConfig {

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter(TenantResolver tenantResolver) {
        FilterRegistrationBean<TenantContextFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new TenantContextFilter(tenantResolver));

        // Suele estar OK: Spring Security corre antes (order menor).
        // Si querés ir a lo seguro, poné un número más alto para que corra después.
        bean.setOrder(1);

        bean.addUrlPatterns("/api/*");
        return bean;
    }
}
