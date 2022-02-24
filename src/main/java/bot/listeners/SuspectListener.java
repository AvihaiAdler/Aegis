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
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      // user doesn't have permission for this command 
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      
      if(event.getMessageContent().split("\\s+").length < 2) return;

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var addedWords = new ArrayList<String>();
      
      // collect the words
      Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' ')).split("\\s+"))
              .stream()
              .filter(word -> !word.isBlank())
              .map(String::toLowerCase)
              .map(String::trim)
              .forEach(word -> {
                if (guild.getSuspiciousWords().add(word)) addedWords.add(word);
              });
      
      dbManager.upsert(guild);
      
      // no words were added - bail;
      if(addedWords.size() == 0) return;
      
      StringBuilder msg = new StringBuilder();
      addedWords.forEach(word -> msg.append("**" + word + "**, "));
      msg.deleteCharAt(msg.lastIndexOf(","));
      
      logger.info("the server " + guild.getId() + " added the following words to their suspicious list " + msg);
      
      // feedback
      new MessageBuilder().setContent("Added the following word\\s to the list:\n" + msg)
              .send(event.getChannel())
              .exceptionally(ExceptionLogger.get());               
       
    }); //event.getMessageAuthor().asUser().ifPresent
  }
}
