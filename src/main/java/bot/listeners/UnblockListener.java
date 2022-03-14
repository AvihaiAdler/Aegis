package bot.listeners;

import java.util.HashSet;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class UnblockListener implements MessageCreateListener {
  private Logger logger = LoggerFactory.getLogger(UnblockListener.class);
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
      
      if(event.getMessageContent().split("\\s+").length < 2) return;

      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        var unblockedUrls = new HashSet<String>();
        
        // collect urls
        Misc.getUrls(event.getMessageContent())
            .stream()
            .map(String::trim)
            .forEach(url -> {
              if (guild.getBlockedUrls().remove(url)) unblockedUrls.add(url);
            });
        var updated = guildDao.save(guild);
        
        // no urls were 'unblocked' - bail
        if(unblockedUrls.size() == 0) return;
        
        StringBuilder msg = new StringBuilder();
        unblockedUrls.forEach(url -> msg.append("- `" + url + "`\n"));
        
        logger.info("the server "  + updated.getGuildName() + " (" + updated.getId() + ")" + " removed the following urls from their block list:\n" + msg);
        
        // feedback
        messageSender.send(event.getChannel(), "Removed the following URLs from the list:\n" + msg, updated)
        .thenRun(() -> event.getMessage().delete()) //delete the command
        .exceptionally(e -> {
          logger.error("failed to delete a message from " + event.getChannel().getId()
                  + " server " + updated.getGuildName() + " (" + updated.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
          return null;
        });
      }); // guild.ifPresent
    }); // event.getMessageAuthor().asUser().ifPresent
  }
}
