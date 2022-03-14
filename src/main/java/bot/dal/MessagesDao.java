package bot.dal;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import bot.data.MessageEntity;

public interface MessagesDao extends MongoRepository<MessageEntity, String>{
  List<MessageEntity> findAllByUsernameAndChannelId(@Param("username") String username, @Param("channelId") String channelId);
  
  List<MessageEntity> findAllBySentTimeMillisBetween( 
          @Param("from") long from,
          @Param("to") long to);
  
  List<MessageEntity> findAllByUsernameAndChannelIdAndSentTimeMillisBetween(
          @Param("username") String username, 
          @Param("channelId") String channelId, 
          @Param("from") long from,
          @Param("to") long to);
}
