package bot.util.Impl;

import org.javacord.api.entity.server.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import bot.util.ChannelCreator;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;

@Component
public class ChannelCreatorImpl implements ChannelCreator {
  private LoggerWrapper loggerWrapper;
  
  @Autowired
  public void setLoggerWrapper(LoggerWrapper loggerWrapper) {
    this.loggerWrapper = loggerWrapper;
  }
  
  @Override
  public String create(Server server, String channelName) {
    // create the channel
    var channels = server.getChannelsByName(channelName);
    if (channels.isEmpty()) {
      return server.createTextChannelBuilder()
        .setName(channelName)
        .create()
        .exceptionally(e -> {
                loggerWrapper.log(Loglevel.ERROR, "failed to create a logging channel for server " + server.getName()
                        + " (" + server.getId() + ")" + "\nreason: " + e.getMessage());
                return null;
        })
        .join()
        .getIdAsString();
    }
    return channels.get(0).getIdAsString();
  }
}
