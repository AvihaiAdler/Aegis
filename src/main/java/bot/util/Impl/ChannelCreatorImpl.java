package bot.util.Impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.server.Server;
import org.springframework.stereotype.Component;
import bot.util.ChannelCreator;
import bot.util.Misc;

@Component
public class ChannelCreatorImpl implements ChannelCreator {
  private Logger logger = LogManager.getLogger();
  
  @Override
  public String create(Server server, String channelName) {
    var name = channelName.toLowerCase();
    var channels = server.getChannelsByName(name);
    
    if (channels.isEmpty()) {
      return server.createTextChannelBuilder()
        .setName(name)
        .create() // create the channel
        .exceptionally(e -> {
                logger.error("failed to create a logging channel for server " + server.getName()
                        + " (" + server.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
                return null;
        })
        .thenApply(ch -> ch.getIdAsString())
        .exceptionally(e -> {
                logger.error("failed to get the id of a logging channel for server " + server.getName()
                        + " (" + server.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
                return null;
        })
        .join();
    }
    
    return channels.get(0).getIdAsString();
  }
}
