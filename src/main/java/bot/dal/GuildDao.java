package bot.dal;

import org.springframework.data.mongodb.repository.MongoRepository;

import bot.data.GuildEntity;

public interface GuildDao extends MongoRepository<GuildEntity, String> {

}
