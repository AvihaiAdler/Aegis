package bot.listeners.Impl;

import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.PrefixListener;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class PrefixListenerImpl implements PrefixListener {
  private Logger logger = LoggerFactory.getLogger(PrefixListenerImpl.class);
  private GuildDao guildDao;
  private MessageSender messageSender;
  
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
      if(!Misc.isUserAllowed(event)) return;
      
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        // process message content
        var splitted = event.getMessageContent().split(" ");
        
        if(splitted.length >= 2) {
          guild.setPrefix(splitted[1]);
          var updated = guildDao.save(guild);
          
          // feedback
          messageSender.send(event.getChannel(), "Prefix has been changed to **" + updated.getPrefix() + "**", updated);
          
          logger.info("prefix for " + updated.getGuildName() + " (" + updated.getId() + ")" + " changed to " + updated.getPrefix());
        }        
      }); // guild.ifPresent
    }); //event.getMessageAuthor().asUser().ifPresent
  }
}
