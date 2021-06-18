package example.person;

import java.util.List;

import org.bson.Document;
import org.springframework.batch.item.ItemWriter;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/** Simple writer */
public class PersonWriter implements ItemWriter<Person>
{
    private final MongoDatabase db;

    public PersonWriter( MongoDatabase db )
    {
        this.db = db;
    }

    @Override
    public void write( List<? extends Person> items ) throws Exception
    {
        MongoCollection<Document> collection = db.getCollection(Person.class.getSimpleName());
        for( Person person : items )
        {
            collection.insertOne(new Document("firstName", person.getFirstName()).append("lastName", person.getLastName()));
        }
    }
}
