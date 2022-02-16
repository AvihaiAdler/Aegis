package bot.listeners;

import java.util.Arrays;
import java.util.HashSet;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import bot.dal.DBManager;

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
      var usrHighestRole = event.getServer().get().getHighestRole(event.getMessageAuthor().asUser().get());
      var botHighestRole = event.getServer().get().getHighestRole(discordApi.getYourself()).get();
      
      // if the user has a lower role than the bot & isn't the server owner - return
      if(!usrHighestRole.isPresent() || 
              (usrHighestRole.get().compareTo(botHighestRole) <= 0 && !event.getServer().get().isOwner(event.getMessageAuthor().asUser().get()))) {
        return;
      }
      if(event.getMessageContent().split("\\s+").length < 2) return;

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var addedWords = new HashSet<String>();
      
      Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' ')).split("\\s+"))
              .stream()
              .map(String::toLowerCase)
              .map(String::trim)
              .forEach(word -> {
                if (guild.getSuspiciousWords().add(word)) addedWords.add(word);
              });
      
      dbManager.update(guild);
      
      if(addedWords.size() > 0 && event.getChannel().canYouWrite()) {
        StringBuilder msg = new StringBuilder();
        addedWords.forEach(word -> msg.append("**" + word + "**, "));
        msg.deleteCharAt(msg.lastIndexOf(","));
        event.getChannel().sendMessage("Added the following word\\s\n" + msg); //exceptionally        
      } 
    }
  }
}
