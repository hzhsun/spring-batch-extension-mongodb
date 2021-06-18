package com.github.nmorel.spring.batch.mongodb;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.client.MongoDatabase;

@Configuration
@PropertySource( "classpath:batch.properties" )
public class ConfigContext
{

    @Autowired
    Environment env;

    @Bean
    static PropertyPlaceholderConfigurer configurer()
    {
        return new PropertyPlaceholderConfigurer();
    }

    @Bean
    MongoDatabase database() throws UnknownHostException
    {
        return new SimpleMongoClientDatabaseFactory("mongodb://localhost:27017/test").getMongoDatabase("test");
//        return client.getDatabase(env.getProperty("mongodb.name"));
    }

}
