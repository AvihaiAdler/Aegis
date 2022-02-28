package bot.util.Impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.server.Server;
import org.springframework.stereotype.Component;

import bot.util.ChannelCreator;

@Component
public class ChannelCreatorImpl implements ChannelCreator {
  private final Logger logger = LogManager.getLogger(RegisterServerImpl.class);
  
  @Override
  public String create(Server server, String channelName) {
    // create the channel
    var channels = server.getChannelsByName(channelName);
    if (channels.isEmpty()) {
      return server.createTextChannelBuilder()
        .setName(channelName)
        .create()
        .exceptionally(e -> {
          logger.error("failed to create a logging channel for server " + server.getId());
          return null;
        })
        .join()
        .getIdAsString();
    }
    return channels.get(0).getIdAsString();
  }
}
