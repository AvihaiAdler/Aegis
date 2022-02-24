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

public class SuspectListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(SuspectListener.class);
  private DBManager dbManager;
  
  public SuspectListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      
      if(event.getMessageContent().split("\\s+").length < 2) return;

      logger.info("invoking " + this.getClass().getName() + " for server " + event.getServer().get());
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var addedWords = new ArrayList<String>();
      
      Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' ')).split("\\s+"))
              .stream()
              .filter(word -> !word.isBlank())
              .map(String::toLowerCase)
              .map(String::trim)
              .forEach(word -> {
                if (guild.getSuspiciousWords().add(word)) addedWords.add(word);
              });
      
      dbManager.upsert(guild);
      
      if(addedWords.size() > 0) {
        StringBuilder msg = new StringBuilder();
        addedWords.forEach(word -> msg.append("**" + word + "**, "));
        msg.deleteCharAt(msg.lastIndexOf(","));
        
        logger.info("the server " + guild.getId() + " added the following words to their suspicious list " + msg);
        
        if(event.getChannel().canYouWrite()) {
          new MessageBuilder().setContent("Added the following word\\s to the list:\n" + msg)
                              .send(event.getChannel())
                              .exceptionally(ExceptionLogger.get());  
//          event.getChannel().sendMessage("Added the following word\\s to the list:\n" + msg).exceptionally(ExceptionLogger.get());                
        }
      } 
    }
  }
}
