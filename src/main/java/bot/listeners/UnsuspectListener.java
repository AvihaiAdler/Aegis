package bot.listeners;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;
import bot.dal.DBManager;
import bot.util.Misc;

public class UnsuspectListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(UnsuspectListener.class);
  private DBManager dbManager;
  
  public UnsuspectListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      
      if(event.getMessageContent().split("\\s+").length < 2) return;

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var removedWords = new ArrayList<String>();
      
      logger.info("invoking " + this.getClass().getName() + " for server " + guild.getId());
      
      Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' ')).split("\\s+"))
              .stream()
              .filter(word -> !word.isBlank())
              .map(String::toLowerCase)
              .map(String::trim)
              .forEach(word -> {             
                if (guild.getSuspiciousWords().remove(word)) removedWords.add(word);
              });
      
      dbManager.upsert(guild);
      
      if(removedWords.size() > 0) {
        StringBuilder msg = new StringBuilder();
        removedWords.forEach(word -> msg.append("**" + word + "**, "));
        msg.deleteCharAt(msg.lastIndexOf(","));
        
        logger.info("the server " + guild.getId() + "removed the following words from their suspicious word list:\n" + msg);
        
        if(event.getChannel().canYouWrite()) {
          new MessageBuilder().setContent("Removed the following word\\s from the list:\n" + msg)
                              .send(event.getChannel())
                              .exceptionally(ExceptionLogger.get()); 
//          event.getChannel()
//            .sendMessage("Removed the following word\\s from the list:\n" + msg)
//            .exceptionally(ExceptionLogger.get());                 
        }
      } 
    }
  }
}
