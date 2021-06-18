package com.github.nmorel.spring.batch.mongodb.incrementer;

import org.springframework.util.Assert;

import com.mongodb.client.MongoDatabase;

/**
 * MongoDB implementation of the {@link ValueIncrementerFactory}
 * interface.
 */
public class MongoDbValueIncrementerFactory implements ValueIncrementerFactory
{
    /** The MongoDB database */
    private MongoDatabase db;

    public MongoDbValueIncrementerFactory( MongoDatabase db )
    {
        Assert.notNull(db, "db must not be null");
        this.db = db;
    }

    @Override
    public ValueIncrementer getIncrementer( String incrementerName )
    {
        Assert.notNull(incrementerName);
        MongoDbValueIncrementer incrementer = new MongoDbValueIncrementer(db, incrementerName);
        incrementer.afterPropertiesSet();
        return incrementer;
    }
}
