package bot.listeners;

import java.util.Arrays;
import java.util.HashMap;
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
      var addedWords = new HashMap<String, String>();
      
      Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' ')).split(","))
              .stream()
              .map(String::trim)
              .filter(word -> word.split(" ").length == 2)
              .forEach(word -> {
                var entry = word.split(" ");
                try {
                  guild.getSuspiciousWords().put(entry[0], Integer.valueOf(entry[1]));
                  addedWords.put(entry[0], entry[1]);                  
                } catch (NumberFormatException e) {
                  //do nothing
                }
              });
      
      dbManager.update(guild);
      
      if(addedWords.size() > 0 && event.getChannel().canYouWrite()) {
        StringBuilder msg = new StringBuilder();
        addedWords.keySet().forEach(key -> msg.append("- **" + key + "** with a value of `" + addedWords.get(key) + "`\n"));
        event.getChannel().sendMessage("added the following word\\s\n" + msg); //exceptionally        
      } 
    }
  }
}
