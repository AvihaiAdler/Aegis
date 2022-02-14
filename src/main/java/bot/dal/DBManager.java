package bot.dal;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import bot.data.GuildEntity;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class DBManager {
  private CodecRegistry codecRegistry;
  private PojoCodecProvider pojoCodecProvider;
  private MongoClient mongoClient;
  private MongoDatabase mongodb;
  private MongoCollection<GuildEntity> mongoCollection;
  
  public DBManager(String connectionString, String dbName, String collectionName) throws Exception {
    pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
    codecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
    mongoClient = MongoClients.create(connectionString);
    mongodb = mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry);
    mongoCollection = mongodb.getCollection(collectionName, GuildEntity.class);
  }
  
  public synchronized void insert(GuildEntity entity) {
    mongoCollection.insertOne(entity);
  }
  
  public synchronized void update(GuildEntity entity) {
    mongoCollection.replaceOne(Filters.eq("_id", entity.getId()), entity);
  }
  
  public synchronized GuildEntity findGuildById(String id) {
    return mongoCollection.find(Filters.eq("_id", id)).first();
  }
  
  public synchronized void delete(String id) {
    mongoCollection.deleteOne(Filters.eq("_id", id));
  }
}
