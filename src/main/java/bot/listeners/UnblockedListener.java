package bot.listeners;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import bot.dal.DBManager;

public class UnblockedListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public UnblockedListener(DBManager dbManager, DiscordApi discordApi) {
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
      var unblockedUrls = new HashSet<String>();
      
      Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' ')).split("\\s+"))
              .stream()
              .filter(this::containsUrl)
              .map(String::trim)
              .forEach(url -> {
                if (guild.getBlockedUrls().remove(url)) unblockedUrls.add(url);
              });
      
      if(event.getChannel().canYouManageMessages()) event.deleteMessage();
      dbManager.update(guild);
      
      if(unblockedUrls.size() > 0 && event.getChannel().canYouWrite()) {
        StringBuilder msg = new StringBuilder();
        unblockedUrls.forEach(url -> msg.append("- `" + url + "`\n"));
        event.getChannel().sendMessage("Removed the following URL\\s from the block list:\n" + msg); //exceptionally        
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
