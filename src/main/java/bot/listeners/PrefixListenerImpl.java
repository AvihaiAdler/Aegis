package bot.listeners;

import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bot.dal.GuildDao;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class PrefixListenerImpl implements PrefixListener {
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
      // user doesn't have permissions for this command
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        // process message content
        var splitted = event.getMessageContent().split(" ");
        
        if(splitted.length >= 2) {
          guild.setPrefix(splitted[1]);
          var updated = guildDao.save(guild);
          
          // feedback
          messageSender.send(event.getChannel(), "Prefix has been changed to **" + updated.getPrefix() + "**", updated);
          
          loggerWrapper.log(Loglevel.INFO, "prefix for " + updated.getGuildName() + " (" + updated.getId() + ")" + " changed to " + updated.getPrefix());
        }        
      }); // guild.ifPresent
    }); //event.getMessageAuthor().asUser().ifPresent
  }
}
