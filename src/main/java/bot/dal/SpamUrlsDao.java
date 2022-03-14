package bot.dal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import bot.data.SpamUrlEntity;

public interface SpamUrlsDao extends MongoRepository<SpamUrlEntity, String> {
  Optional<SpamUrlEntity> findOneByUrl(@Param("url") String url);
  
  List<SpamUrlEntity> findAllBySentDateBetween(@Param("from") long from, @Param("to") long to);
}
