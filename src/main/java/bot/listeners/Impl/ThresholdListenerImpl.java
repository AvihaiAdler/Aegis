package bot.listeners.Impl;

import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.ThresholdListener;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class ThresholdListenerImpl implements ThresholdListener {
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
      if(!Misc.isUserAllowed(event, event.getApi())) return;

      if(event.getMessageContent().split("\\s+").length < 2) return;
      
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {       
        // process the command
        try {
          var newThreshold = Integer.valueOf(event.getMessageContent().split("\\s+")[1]);
          // threshold must be 0 or higher
          if(newThreshold >= 0) {
            guild.setThreshold(newThreshold);
            var updated = guildDao.save(guild);
            
            loggerWrapper.log(Loglevel.INFO, "the server " + updated.getGuildName() + " (" + updated.getId() + ")" + " changed their prefix to " + updated.getPrefix());
            
            // feedback
            messageSender.send(event.getChannel(), "Threshold changed to **" + updated.getThreshold() + "**", updated);      
          }
        } catch (NumberFormatException e) {
          loggerWrapper.log(Loglevel.ERROR, e.getMessage());
        }        
      }); // guild.ifPresent
    }); // event.getMessageAuthor().asUser().ifPresent
  }
}
