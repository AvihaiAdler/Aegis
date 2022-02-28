package bot.listeners;

import java.util.Arrays;
import java.util.HashSet;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class UnblockListenerImpl implements UnblockListener {
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
        var unblockedUrls = new HashSet<String>();
        
        // collect urls
        Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' '))
                .split("\\s+"))
                .stream()
                .filter(Misc::containsUrl)
                .map(String::trim)
                .forEach(url -> {
                  if (guild.getBlockedUrls().remove(url)) unblockedUrls.add(url);
                });
        var updated = guildDao.save(guild);
        
        // no urls were 'unblocked' - bail
        if(unblockedUrls.size() == 0) return;
        
        StringBuilder msg = new StringBuilder();
        unblockedUrls.forEach(url -> msg.append("- `" + url + "`\n"));
        
        loggerWrapper.log(Loglevel.INFO, "the server "  + updated.getGuildName() + " (" + updated.getId() + ")" + " removed the following urls from their block list:\n" + msg);
        
        // feedback
        messageSender.send(event.getChannel(), "Removed the following URLs from the list:\n" + msg, updated)
        .thenRun(() -> event.getMessage().delete()) //delete the command
        .exceptionally(e -> {
          loggerWrapper.log(Loglevel.ERROR, "failed to delete a message from " + event.getChannel().getId()
                  + " server " + updated.getGuildName() + " (" + updated.getId() + ")" + "\nreason: " + e.getMessage());
          return null;
        });
      }); // guild.ifPresent
    }); // event.getMessageAuthor().asUser().ifPresent
  }
}
