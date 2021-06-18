package example.person;

 import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.mongodb.client.MongoDatabase;

@Configuration
public class PersonContext
{
    @Bean
    ItemReader<Person> itemReader()
    {
        FlatFileItemReader<Person> reader = new FlatFileItemReader<Person>();
        reader.setResource(new ClassPathResource("support/sample-data.csv"));
        reader.setLineMapper(new DefaultLineMapper<Person>()
        {{
                setLineTokenizer(new DelimitedLineTokenizer()
                {{
                        setNames(new String[]{"firstName", "lastName"});
                    }});
                setFieldSetMapper(new BeanWrapperFieldSetMapper<Person>()
                {{
                        setTargetType(Person.class);
                    }});
            }});
        return reader;
    }

    @Bean
    PersonItemProcessor itemProcess()
    {
        return new PersonItemProcessor();
    }

    @Bean
    ItemWriter<Person> itemWriter( MongoDatabase db )
    {
        return new PersonWriter(db);
    }

    @Bean
    public Job personJob( JobBuilderFactory jobs, Step step1 )
    {
        return jobs.get("personJob")
                .incrementer(new RunIdIncrementer())
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step step1( StepBuilderFactory stepBuilderFactory, ItemReader<Person> itemReader,
                       ItemWriter<Person> itemWriter, ItemProcessor<Person, Person> itemProcess )
    {
        return stepBuilderFactory.get("step1")
                .<Person, Person>chunk(10)
                .reader(itemReader)
                .processor(itemProcess)
                .writer(itemWriter)
                .build();
    }

}
