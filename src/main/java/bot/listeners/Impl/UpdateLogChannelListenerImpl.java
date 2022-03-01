package bot.listeners.Impl;

import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.UpdateLogChannelListener;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class UpdateLogChannelListenerImpl implements UpdateLogChannelListener {
  private Logger logger = LoggerFactory.getLogger(UpdateLogChannelListenerImpl.class);
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
      if (!Misc.isUserAllowed(event)) return;
      
      var content = event.getMessageContent().split("\\s+");
      if (content.length < 2) return;

      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        // change the channel
        if (Misc.channelExists(content[1], event.getServer().get())) {
          guild.setLogChannelId(content[1]);
          var updated = guildDao.save(guild);
          
          logger.info("the server " + guild.getGuildName() + " ("+ guild.getId() + ")" + " changed their logging channel to " + guild.getLogChannelId());
          
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
