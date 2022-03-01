package bot.util.Impl;

import org.javacord.api.entity.server.Server;
import org.javacord.api.event.Event;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.server.ServerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import bot.dal.GuildDao;
import bot.data.GuildEntity;
import bot.listeners.Impl.UrlListenerImpl;
import bot.util.ChannelCreator;
import bot.util.RegisterServer;

@Component
public class RegisterServerImpl implements RegisterServer {
  private Logger logger = LoggerFactory.getLogger(UrlListenerImpl.class);
  private GuildDao guildDao;
  private ChannelCreator channelCreator;
  
  @Autowired
  public void setGuildDao(GuildDao guildDao) {
    this.guildDao = guildDao;
  }
  
  @Autowired
  public void setChannelCreator(ChannelCreator channelCreator) {
    this.channelCreator = channelCreator;
  }
  
  @Override
  public void registerServer(Event event) {
    Server server = null;
    if (event instanceof ServerEvent) {
      server = ((ServerEvent) event).getServer();
    } else if (event instanceof MessageCreateEvent) {
      server = ((MessageCreateEvent) event).getServer().get();
    } else {
      logger.error("unsupported event detected");
      return;
    }

    final var s = server;
    guildDao.findById(server.getIdAsString()).ifPresentOrElse(guild -> {
      // create a logging channel
      guild.setLogChannelId(channelCreator.create(s, event.getApi().getYourself().getName() + "-log"));
      
      // register the server
      var entity = guildDao.save(guild);
      
      logger.info("registered server: " + entity.getGuildName() + "(" + entity.getId() + ")" + " logging channel: " + entity.getLogChannelId());
    }, 
    () -> {
      // create a logging channel
      var logChannelId = channelCreator.create(s, event.getApi().getYourself().getName() + "-log");
      
      // register the server
      var entity = guildDao.save(new GuildEntity(s.getIdAsString(), s.getName(), logChannelId));
      
      logger.info("registered server: " + entity.getGuildName() + "(" + entity.getId() + ")" + " logging channel: " + entity.getLogChannelId());
    });
  }
}
