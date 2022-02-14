package bot.listeners;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import bot.dal.DBManager;

public class SpamListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public SpamListener(DBManager mongoClient, DiscordApi discordApi) {
    this.dbManager = mongoClient;
    this.discordApi = discordApi;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if (event.getServer().isPresent() && !event.getMessageAuthor().isBotUser()) {
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if (guild == null)
        return;

      if(event.getMessageAuthor().asUser().isPresent()) {
        var usrHighestRole = event.getServer().get().getHighestRole(event.getMessageAuthor().asUser().get()).get();
        var botHighestRole = event.getServer().get().getHighestRole(discordApi.getYourself()).get();
        
        // check if the user has higher role than the bot      
        if(usrHighestRole.compareTo(botHighestRole) > 0) return;

        //check if the user is the server owner
        if(event.getServer().get().isOwner(event.getMessageAuthor().asUser().get())) return;
      }
      
      // if the guild is 'restricted' 
      if (guild.getRestricted() && event.getMessage().mentionsEveryone()) {
        if(!event.getMessage().getEmbeds().isEmpty() || containsUrl(event.getMessageContent())) {
          event.deleteMessage();
        }
      }
    }
  }
  
  private boolean containsUrl(String str) {
    final String urlRegex = "<?\\b(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>?";
    if(str == null)
      return false;
    var content = Arrays.asList(str.split("\\s+")).stream()
      .filter(word -> Pattern.matches(urlRegex, word))
      .collect(Collectors.toList()); 
    return !content.isEmpty();
  } 
}
