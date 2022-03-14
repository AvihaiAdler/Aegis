package bot.listeners;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bot.dal.GuildDao;
import bot.util.MessageSender;

@Service
public class NotifyListener implements MessageCreateListener {
  private Logger logger = LoggerFactory.getLogger(NotifyListener.class);
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
      if(!usr.isBotOwner()) return;
      
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guildEntity -> {
        var messageContent = event.getMessageContent();
        final var msg = messageContent.substring((guildEntity.getPrefix() + "notify").length()).trim();
        
        // notify each guild with msg
        guildDao.findAll().forEach(guild -> {
          event.getApi().getServerTextChannelById(guild.getLogChannelId()).ifPresent(serverChannel -> {
            logger.info("notifying server " + guild.getGuildName() + " (" + guild.getId() + ") with message " + msg);
            messageSender.send(serverChannel, msg, guildEntity);
          }); // getServerTextChannelById().ifPresent
        }); //forEach
      }); //event.getMessageAuthor().asUser().ifPresent        
    });
  }
}
