package bot.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

/*
 * TODO
 * exceptionally if needed
 */
public class SuspectListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public SuspectListener(DBManager dbManager, DiscordApi discordApi) {
    this.dbManager = dbManager;
    this.discordApi = discordApi;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isAllowed(event, discordApi)) return;
      
      if(event.getMessageContent().split("\\s+").length < 2) return;

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
      
      if(addedWords.size() > 0 && event.getChannel().canYouWrite()) {
        StringBuilder msg = new StringBuilder();
        addedWords.forEach(word -> msg.append("**" + word + "**, "));
        msg.deleteCharAt(msg.lastIndexOf(","));
        event.getChannel().sendMessage("Added the following word\\s to the list:\n" + msg).exceptionally(ExceptionLogger.get());      
      } 
    }
  }
}
