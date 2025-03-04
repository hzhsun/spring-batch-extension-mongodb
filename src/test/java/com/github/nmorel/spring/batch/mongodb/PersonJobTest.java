package com.github.nmorel.spring.batch.mongodb;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.client.MongoDatabase;

import example.person.PersonContext;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( classes = {TestContext.class, PersonContext.class} )
public class PersonJobTest
{

    @Autowired
    private Job personJob;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private MongoDatabase db;

    @Autowired
    private JobRepository repository;

    @Before
    public void onSetUpInTransaction() throws Exception
    {
        db.drop();
    }

    @After
    public void tearDown()
    {
        db.drop();
    }

    @Test
    public void testPersonJob()
            throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException
    {
        JobExecution jobExecution = jobLauncher.run(personJob, new JobParameters());
        assertEquals("Batch status not COMPLETED", BatchStatus.COMPLETED, jobExecution.getStatus());

        JobExecution lastJobExecution = repository.getLastJobExecution(personJob.getName(), new JobParameters());
        assertEquals("Last job execution not equals to the job execution returned", jobExecution.getId(), lastJobExecution.getId());

        assertEquals("The step didn't run once", 1, repository.getStepExecutionCount(jobExecution.getJobInstance(), "step1"));

        StepExecution stepExecution = repository.getLastStepExecution(jobExecution.getJobInstance(), "step1");
        assertEquals("Step status not COMPLETED", BatchStatus.COMPLETED, stepExecution.getStatus());
        assertEquals("Step read count not 5", 5, stepExecution.getReadCount());
        assertEquals("Step write count not 5", 5, stepExecution.getWriteCount());
        assertEquals("Step commit count not 1", 1, stepExecution.getCommitCount());
        assertEquals("Step filter count not 0", 0, stepExecution.getFilterCount());
        assertEquals("Step rollback count not 0", 0, stepExecution.getRollbackCount());
        assertEquals("Step skip count not 0", 0, stepExecution.getSkipCount());
        assertEquals("Step process skip count not 0", 0, stepExecution.getProcessSkipCount());
        assertEquals("Step read skip count not 0", 0, stepExecution.getReadSkipCount());
        assertEquals("Step write skip count not 0", 0, stepExecution.getWriteSkipCount());

    }
}
