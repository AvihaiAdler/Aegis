package bot.listeners.Impl;

import java.util.HashSet;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.BlockListener;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class BlockListenerImpl implements BlockListener {
  private Logger logger = LoggerFactory.getLogger(BlockListenerImpl.class);
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
      if(!Misc.isUserAllowed(event)) return;
      if(event.getMessageContent().split("\\s+").length < 2) return;

      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        var blockedUrls = new HashSet<String>();
        // collect all the urls from the message
        Misc.getUrls(event.getMessageContent())
            .stream()
            .map(String::trim)
            .forEach(url -> {
              if (guild.getBlockedUrls().add(url)) blockedUrls.add(url);
            });
        
        var updated = guildDao.save(guild);
        
        // no urls were added - bail
        if (blockedUrls.size() == 0)
          return;
        
        StringBuilder msg = new StringBuilder();
        blockedUrls.forEach(url -> msg.append("- `" + url + "`\n"));
        
        logger.info("the server " + updated.getId() + " added the following to their block list:\n" + msg);
        
        // feedback
        messageSender.send(event.getChannel(), "The following URLs have been added into the list:\n" + msg, updated)
        .thenRun(() -> event.getMessage().delete()) // delete the command to prevent clutter
        .exceptionally(e -> {
          logger.error("failed to delete a message from " + event.getChannel().getId() + " server "
                  + updated.getGuildName() + " (" + updated.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
          return null;
        });         
      }); // guild.ifPresent
    }); //event.getMessageAuthor().asUser().ifPresent
  }
}
