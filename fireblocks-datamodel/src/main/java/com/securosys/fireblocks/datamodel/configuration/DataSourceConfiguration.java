// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.datamodel.configuration;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager",
        basePackages = {"com.securosys.fireblocks.business.repository"}
)
public class DataSourceConfiguration {

    private static String url;
    private static String username;
    private static String password;

    @Value("${spring.datasource.url:}")
    public void setUrl(String url) {
        this.url = url;
    }

    @Value("${spring.datasource.username:}")
    public void setUsername(String username) {
        this.username = username;
    }


    @Value("${spring.datasource.password:}")
    public void setPassword(String password) {
        this.password = password;
    }

    @Value("${spring.datasource.poolName:HikariPool}")
    public String datasourcePoolName;

    @Value("${spring.datasource.maximumPoolSize:10}")
    public int datasourceMaximumPoolSize;

    private static final String postgresMigrationFilePath = "classpath:com/securosys/fireblocks/datamodel/postgresql/migration";
    private static final String mariadbMigrationFilePath = "classpath:com/securosys/fireblocks/datamodel/mariadb/migration";
    private static final String postgresDriverClassName = "org.postgresql.Driver";
    private static final String mariadbDriverClassName = "org.mariadb.jdbc.Driver";
    private static final String h2DriverClassName = "org.h2.Driver";

    /* SFE: Removed since, flyway > 10.0.0 (no longer required)
    @PostConstruct
    public static void migrateFlyway() {

    }*/

    @Bean(name = "dataSource")
    public DataSource initializeFirstDataSource() throws Exception {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setPoolName(datasourcePoolName);
        dataSource.setMaximumPoolSize(datasourceMaximumPoolSize);

        Map<String, String> placeholders1 = new HashMap<>();
        placeholders1.put("username", username);

        if (url.contains("jdbc:postgresql:")) {
            dataSource.setDriverClassName(postgresDriverClassName);
            migrateFlyway(dataSource, postgresMigrationFilePath, placeholders1);
        } else if (url.contains("jdbc:mariadb:")) {
            dataSource.setDriverClassName(mariadbDriverClassName);
            migrateFlyway(dataSource, mariadbMigrationFilePath, placeholders1);
        } else if (url.contains("jdbc:h2")){
            dataSource.setDriverClassName(h2DriverClassName);
            migrateFlyway(dataSource, mariadbMigrationFilePath, placeholders1);
        } else {
            throw new Exception("Unsupported database url, expecting spring.datasource.url to contain with (jdbc:postgresql: or jdbc:mariadb:)");
        }

        return dataSource;
    }

    private static void migrateFlyway(DataSource dataSource, String migrationFilePath, Map<String, String> placeholders1){
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(migrationFilePath)
                .validateOnMigrate(true)
                .baselineOnMigrate(false)
                .table("schema_version")
                .sqlMigrationPrefix("v")
                .placeholders(placeholders1)
                .load();
        flyway.migrate();
    }


    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                       @Qualifier("dataSource") DataSource dataSource) throws Exception {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(initializeFirstDataSource());
        em.setPackagesToScan("com.securosys.fireblocks.datamodel.entities");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        em.setDataSource(dataSource);
        em.setPersistenceUnitName("SECUROSYS-UNIT");

        return em;
    }


    @Bean(name = "transactionManager")
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public static PersistenceExceptionTranslationPostProcessor exceptionTranslation(){
        return new PersistenceExceptionTranslationPostProcessor();
    }


    static Properties additionalProperties() {
        Properties properties = new Properties();
        //properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");

        return properties;
    }

}
