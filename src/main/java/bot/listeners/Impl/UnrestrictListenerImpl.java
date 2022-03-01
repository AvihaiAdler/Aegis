package bot.listeners.Impl;

import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.UnrestrictListener;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class UnrestrictListenerImpl implements UnrestrictListener {
  private Logger logger = LoggerFactory.getLogger(UnrestrictListenerImpl.class);
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
      // user doesn't have permission for this command
      if(!Misc.isUserAllowed(event)) return;
      
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        if(guild.getRestricted()) {
          guild.setRestricted(false);
          var updated = guildDao.save(guild);
          
          logger.info("the server " + updated.getGuildName() + " (" + updated.getId() + ")" + " is no longer restricted");
          
          // feedback
          messageSender.send(event.getChannel(), "The server is no longer in restrict mode", updated);  
        }        
      }); // guild.ifPresent
    }); // event.getMessageAuthor().asUser().ifPresent
  }
}
