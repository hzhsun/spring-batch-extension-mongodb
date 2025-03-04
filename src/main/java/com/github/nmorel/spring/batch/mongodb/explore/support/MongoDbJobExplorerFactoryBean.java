package com.github.nmorel.spring.batch.mongodb.explore.support;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.AbstractJobExplorerFactoryBean;
import org.springframework.batch.core.explore.support.SimpleJobExplorer;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.github.nmorel.spring.batch.mongodb.incrementer.ValueIncrementer;
import com.github.nmorel.spring.batch.mongodb.repository.dao.AbstractMongoDbDao;
import com.github.nmorel.spring.batch.mongodb.repository.dao.MongoDbExecutionContextDao;
import com.github.nmorel.spring.batch.mongodb.repository.dao.MongoDbJobExecutionDao;
import com.github.nmorel.spring.batch.mongodb.repository.dao.MongoDbJobInstanceDao;
import com.github.nmorel.spring.batch.mongodb.repository.dao.MongoDbStepExecutionDao;
import com.mongodb.client.MongoDatabase;

/** Implementation of {@link AbstractJobExplorerFactoryBean} */
public class MongoDbJobExplorerFactoryBean extends AbstractJobExplorerFactoryBean implements InitializingBean
{
    private MongoDatabase db;

    private String collectionPrefix = AbstractMongoDbDao.DEFAULT_COLLECTION_PREFIX;

    private ValueIncrementer incrementer = new ValueIncrementer()
    {
        @Override
        public int nextIntValue()
        {
            throw new IllegalStateException("JobExplorer is read only.");
        }

        @Override
        public long nextLongValue()
        {
            throw new IllegalStateException("JobExplorer is read only.");
        }

        @Override
        public String nextStringValue()
        {
            throw new IllegalStateException("JobExplorer is read only.");
        }
    };

    private ExecutionContextSerializer serializer;

    /**
     * A custom implementation of the {@link ExecutionContextSerializer}.
     * The default, if not injected, is the {@link org.springframework.batch.core.repository.dao.XStreamExecutionContextStringSerializer}.
     *
     * @see ExecutionContextSerializer
     */
    public void setSerializer( ExecutionContextSerializer serializer )
    {
        this.serializer = serializer;
    }

    /**
     * Public setter for the {@link DB}.
     *
     * @param db a {@link DB}
     */
    public void setDb( MongoDatabase db )
    {
        this.db = db;
    }

    /** Sets the collection prefix for all the batch meta-data tables. */
    public void setCollectionPrefix( String collectionPrefix )
    {
        this.collectionPrefix = collectionPrefix;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        Assert.notNull(db, "db must not be null.");

        if( serializer == null )
        {
            Jackson2ExecutionContextStringSerializer defaultSerializer = new Jackson2ExecutionContextStringSerializer();
//            defaultSerializer.afterPropertiesSet();

            serializer = defaultSerializer;
        }
    }

    private JobExplorer getTarget() throws Exception
    {
        return new SimpleJobExplorer(createJobInstanceDao(),
                createJobExecutionDao(), createStepExecutionDao(),
                createExecutionContextDao());
    }

    @Override
    protected ExecutionContextDao createExecutionContextDao() throws Exception
    {
        MongoDbExecutionContextDao dao = new MongoDbExecutionContextDao();
        dao.setDb(db);
        dao.setSerializer(serializer);
        dao.setPrefix(collectionPrefix);
        dao.afterPropertiesSet();
        return dao;
    }

    @Override
    protected StepExecutionDao createStepExecutionDao() throws Exception
    {
        MongoDbStepExecutionDao dao = new MongoDbStepExecutionDao();
        dao.setDb(db);
        dao.setPrefix(collectionPrefix);
        dao.setStepExecutionIncrementer(incrementer);
        dao.afterPropertiesSet();
        return dao;
    }

    @Override
    protected JobExecutionDao createJobExecutionDao() throws Exception
    {
        MongoDbJobExecutionDao dao = new MongoDbJobExecutionDao();
        dao.setDb(db);
        dao.setPrefix(collectionPrefix);
        dao.setJobExecutionIncrementer(incrementer);
        dao.afterPropertiesSet();
        return dao;
    }

    @Override
    protected JobInstanceDao createJobInstanceDao() throws Exception
    {
        MongoDbJobInstanceDao dao = new MongoDbJobInstanceDao();
        dao.setPrefix(collectionPrefix);
        dao.setDb(db);
        dao.setJobIncrementer(incrementer);
        dao.afterPropertiesSet();
        return dao;
    }

    @Override
    public JobExplorer getObject() throws Exception
    {
        return getTarget();
    }
}
