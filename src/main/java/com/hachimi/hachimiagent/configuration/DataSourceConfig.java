package com.hachimi.hachimiagent.configuration;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 多数据源配置 - 修复版本，匹配现有配置格式
 */
@Configuration
public class DataSourceConfig {

    /**
     * 主数据源 - MySQL (对话存储)
     * 使用现有的配置路径
     */
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties("spring.datasource")  // 使用现有的配置路径
    public DataSource primaryDataSource() {
        return new DruidDataSource();
    }

    /**
     * 向量数据源 - PostgreSQL (RAG知识库)
     * 使用新的配置路径
     */
    @Bean(name = "vectorDataSource")
    @ConfigurationProperties("spring.datasource.vector")  // 简化的配置路径
    public DataSource vectorDataSource() {
        return new DruidDataSource();
    }

    /**
     * 主数据源JdbcTemplate - MySQL
     */
    @Primary
    @Bean(name = "primaryJdbcTemplate")
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 向量数据源JdbcTemplate - PostgreSQL (用于pgvector)
     */
    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 主数据源事务管理器 - MySQL
     */
    @Primary
    @Bean(name = "primaryTransactionManager")
    public DataSourceTransactionManager primaryTransactionManager(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 向量数据源事务管理器 - PostgreSQL
     */
    @Bean(name = "vectorTransactionManager")
    public DataSourceTransactionManager vectorTransactionManager(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}