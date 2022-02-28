package bot.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class UnsuspectListenerImpl implements UnsuspectListener {
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
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      
      if(event.getMessageContent().split("\\s+").length < 2) return;
      
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        var removedWords = new ArrayList<String>();
        
        // collect the words
        Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' '))
                .split("\\s+"))
                .stream()
                .filter(word -> !word.isBlank())
                .map(String::toLowerCase)
                .map(String::trim)
                .forEach(word -> {
                  if (guild.getSuspiciousWords().remove(word)) removedWords.add(word);
                });
        
        var updated = guildDao.save(guild);
        
        if(removedWords.size() > 0) {
          StringBuilder msg = new StringBuilder();
          removedWords.forEach(word -> msg.append("**" + word + "**, "));
          msg.deleteCharAt(msg.lastIndexOf(","));
          
          loggerWrapper.log(Loglevel.INFO, "the server " + updated.getGuildName() + " (" + updated.getId() + ")" + "removed the following words from their suspicious word list:\n" + msg);
          
          // feedback
          messageSender.send(event.getChannel(), "Removed the following words from the list:\n" + msg, updated);
        } 
      }); // guild.ifPresent
    }); // event.getMessageAuthor().asUser().ifPresent
  }
}
