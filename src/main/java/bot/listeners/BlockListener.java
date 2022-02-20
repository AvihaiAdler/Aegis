package bot.listeners;

import java.util.Arrays;
import java.util.HashSet;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class BlockListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public BlockListener(DBManager dbManager, DiscordApi discordApi) {
    this.dbManager = dbManager;
    this.discordApi = discordApi;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
//      var usrHighestRole = event.getServer().get().getHighestRole(event.getMessageAuthor().asUser().get());
//      var botHighestRole = event.getServer().get().getHighestRole(discordApi.getYourself()).get();
//      
//      // if the user has a lower role than the bot & isn't the server owner - return
//      if(!usrHighestRole.isPresent() || 
//              (usrHighestRole.get().compareTo(botHighestRole) <= 0 &&
//              !event.getServer().get().isOwner(event.getMessageAuthor().asUser().get()))) return;
      if(!Misc.isAllowed(event, discordApi)) return;
      
      if(event.getMessageContent().split("\\s+").length < 2) return;

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var blockedUrls = new HashSet<String>();
      
      Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' ')).split("\\s+"))
              .stream()
              .filter(Misc::containsUrl)
              .map(String::trim)
              .forEach(url -> {
                if (guild.getBlockedUrls().add(url)) blockedUrls.add(url);
              });
      
      if(event.getChannel().canYouManageMessages()) event.deleteMessage();
      dbManager.upsert(guild);
      
      if(blockedUrls.size() > 0 && event.getChannel().canYouWrite()) {
        StringBuilder msg = new StringBuilder();
        blockedUrls.forEach(url -> msg.append("- `" + url + "`\n"));
        event.getChannel().sendMessage("The following URL\\s have been added to the list:\n" + msg).exceptionally(ExceptionLogger.get());       
      } 
    }
  }
}
