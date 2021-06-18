package com.github.nmorel.spring.batch.mongodb.repository.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.util.Assert;

import com.github.nmorel.spring.batch.mongodb.incrementer.ValueIncrementer;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;


/** {@link org.springframework.batch.core.repository.dao.JobInstanceDao} implementation for MongoDB */
public class MongoDbJobInstanceDao extends AbstractMongoDbDao implements JobInstanceDao
{
    static final String COLLECTION_NAME = JobInstance.class.getSimpleName();

    private static final String JOB_KEY_KEY = "jobKey";

    private ValueIncrementer jobIncrementer;

    private JobKeyGenerator<JobParameters> jobKeyGenerator = new DefaultJobKeyGenerator();

    /**
     * Setter for {@link ValueIncrementer} to be used when
     * generating primary keys for {@link JobInstance} instances.
     *
     * @param jobIncrementer the {@link ValueIncrementer}
     */
    public void setJobIncrementer( ValueIncrementer jobIncrementer )
    {
        this.jobIncrementer = jobIncrementer;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        super.afterPropertiesSet();
        Assert.notNull(jobIncrementer, "The jobIncrementer must not be null.");
        getCollection().createIndex(new BasicDBObject(JOB_INSTANCE_ID_KEY, 1L));
    }

    @Override
    protected String getCollectionName()
    {
        return COLLECTION_NAME;
    }

    @Override
    public JobInstance createJobInstance( String jobName, JobParameters jobParameters )
    {
        Assert.notNull(jobName, "Job name must not be null.");
        Assert.notNull(jobParameters, "JobParameters must not be null.");

        Assert.state(getJobInstance(jobName, jobParameters) == null,
                "JobInstance must not already exist");

        Long jobId = jobIncrementer.nextLongValue();

        JobInstance jobInstance = new JobInstance(jobId, jobName);
        jobInstance.incrementVersion();

        getCollection().insertOne(start()
                .append(JOB_INSTANCE_ID_KEY, jobId)
                .append(JOB_NAME_KEY, jobName)
                .append(JOB_KEY_KEY, jobKeyGenerator.generateKey(jobParameters))
                .append(VERSION_KEY, jobInstance.getVersion()));

        return jobInstance;
    }

    @Override
    public JobInstance getJobInstance( String jobName, JobParameters jobParameters )
    {
        Assert.notNull(jobName, "Job name must not be null.");
        Assert.notNull(jobParameters, "JobParameters must not be null.");

        String jobKey = jobKeyGenerator.generateKey(jobParameters);

        return mapJobInstance(getCollection().find(start()
                .append(JOB_NAME_KEY, jobName)
                .append(JOB_KEY_KEY, jobKey)).first());
    }

    @Override
    public JobInstance getJobInstance( Long instanceId )
    {
        return mapJobInstance(getCollection().find(new BasicDBObject(JOB_INSTANCE_ID_KEY, instanceId)).first());
    }

    @Override
    public JobInstance getJobInstance( JobExecution jobExecution )
    {
        Document instanceId = getCollection(MongoDbJobExecutionDao.COLLECTION_NAME)
                .find(new BasicDBObject(JOB_EXECUTION_ID_KEY, jobExecution.getId())).first();
        return mapJobInstance(getCollection().find(new BasicDBObject(JOB_INSTANCE_ID_KEY, instanceId.get(JOB_INSTANCE_ID_KEY))).first());
    }

    @Override
    public List<JobInstance> getJobInstances( String jobName, int start, int count )
    {
        return mapJobInstances(getCollection().find(new BasicDBObject(JOB_NAME_KEY, jobName)).sort(new BasicDBObject(JOB_INSTANCE_ID_KEY, -1L))
                .skip(start).limit(count).cursor());
    }

    @Override
    public List<String> getJobNames()
    {
        List<String> results = new ArrayList<String>(); 
        		getCollection().distinct(JOB_NAME_KEY, String.class).forEach(job->results.add(job));
        Collections.sort(results);
        return results;
    }

    @Override
    public List<JobInstance> findJobInstancesByName(String jobName, int start, int count)
    {
        return mapJobInstances(getCollection().find(new BasicDBObject(JOB_NAME_KEY, Pattern.compile(jobName))).sort(new BasicDBObject(JOB_INSTANCE_ID_KEY, -1L))
                .skip(start).limit(count).cursor());
    }

    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException
    {
        return (int) getCollection().countDocuments(new Document(JOB_NAME_KEY, jobName));
    }

    private List<JobInstance> mapJobInstances( MongoCursor<Document> dbCursor )
    {
        List<JobInstance> results = new ArrayList<JobInstance>();
        while( dbCursor.hasNext() )
        {
            results.add(mapJobInstance(dbCursor.next()));
        }
        dbCursor.close();
        return results;
    }

    private JobInstance mapJobInstance( Document dbObject )
    {
        if( dbObject == null )
        {
            return null;
        }
        JobInstance jobInstance = new JobInstance((Long) dbObject.get(JOB_INSTANCE_ID_KEY), (String) dbObject.get(JOB_NAME_KEY));
        // should always be at version=0 because they never get updated
        jobInstance.incrementVersion();
        return jobInstance;
    }
}
