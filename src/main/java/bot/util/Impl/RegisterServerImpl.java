package bot.util.Impl;

import org.javacord.api.entity.server.Server;
import org.javacord.api.event.Event;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.server.ServerEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bot.dal.GuildDao;
import bot.data.GuildEntity;
import bot.util.ChannelCreator;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.RegisterServer;

@Component
public class RegisterServerImpl implements RegisterServer {
  private GuildDao guildDao;
  private ChannelCreator channelCreator;
  private LoggerWrapper loggerWrapper;
  
  @Autowired
  public void setGuildDao(GuildDao guildDao) {
    this.guildDao = guildDao;
  }
  
  @Autowired
  public void setChannelCreator(ChannelCreator channelCreator) {
    this.channelCreator = channelCreator;
  }
  
  @Autowired
  public void setLoggerWrapper(LoggerWrapper loggerWrapper) {
    this.loggerWrapper = loggerWrapper;
  }
  
  @Override
  public void registerServer(Event event) {
    Server server = null;
    if (event instanceof ServerEvent) {
      server = ((ServerEvent) event).getServer();
    } else if (event instanceof MessageCreateEvent) {
      server = ((MessageCreateEvent) event).getServer().get();
    } else {
      loggerWrapper.log(Loglevel.ERROR, "unsupported event detected");
      return;
    }

    final var s = server;
    guildDao.findById(server.getIdAsString()).ifPresentOrElse(guild -> {
      // create a logging channel
      guild.setLogChannelId(channelCreator.create(s, event.getApi().getYourself().getName()));
      
      // register the server
      var entity = guildDao.save(guild);
      
      loggerWrapper.log(Loglevel.INFO, "registered server: " + entity.getGuildName() + "(" + entity.getId() + ")" + " logging channel: " + entity.getLogChannelId());
    }, 
    () -> {
      // create a logging channel
      var logChannelId = channelCreator.create(s, event.getApi().getYourself().getName());
      
      // register the server
      var entity = guildDao.save(new GuildEntity(s.getIdAsString(), s.getName(), logChannelId));
      
      loggerWrapper.log(Loglevel.INFO, "registered server: " + entity.getGuildName() + "(" + entity.getId() + ")" + " logging channel: " + entity.getLogChannelId());
    });
  }
}
