package com.inventory.config.mybatis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

/**
 * MyBatis-Plus 分页插件（按数据库类型拆分，供双数据源分别挂载）。
 *
 * @author inventory
 */
@Configuration
public class MybatisPlusInterceptorConfiguration {


    @Bean(name = "pgsqlPaginationInterceptor")
    public MybatisPlusInterceptor pgsqlPaginationInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
