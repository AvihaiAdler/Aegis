package bot.listeners.Impl;

import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.UpdateLogChannelListener;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class UpdateLogChannelListenerImpl implements UpdateLogChannelListener {
  private LoggerWrapper loggerWrapper;
  private GuildDao guildDao;
  private MessageSender messageSender;
  
  @Autowired
  public void setLoggerWrapper(LoggerWrapper loggerWrapper) {
    this.loggerWrapper = loggerWrapper;
  }

  @Autowired
  public void setGuildDao(GuildDao guildDao) {
    this.guildDao = guildDao;
  }
  
  @Autowired
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {    
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      // user doesn't have permission for this command
      if (!Misc.isUserAllowed(event, event.getApi())) return;
      
      var content = event.getMessageContent().split("\\s+");
      if (content.length < 2) return;

      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        // change the channel
        if (Misc.channelExists(content[1], event.getServer().get())) {
          guild.setLogChannelId(content[1]);
          var updated = guildDao.save(guild);
          
          loggerWrapper.log(Loglevel.INFO, "the server " + guild.getGuildName() + " ("+ guild.getId() + ")" + " changed their logging channel to " + guild.getLogChannelId());
          
          // feedback
          var msg = "Logs will appear at **#" + event.getServer()
              .get()
              .getChannelById(updated.getLogChannelId())
              .get()
              .getName() + "**";
          messageSender.send(event.getChannel(), msg, updated);
        }      
      }); // guild.ifPresent    
    }); // event.getMessageAuthor().asUser().ifPresent
  }
}
