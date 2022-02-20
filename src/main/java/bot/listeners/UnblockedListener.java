package bot.listeners;

import java.util.Arrays;
import java.util.HashSet;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

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
      if(!Misc.isAllowed(event, discordApi)) return;
      
      if(event.getMessageContent().split("\\s+").length < 2) return;

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var unblockedUrls = new HashSet<String>();
      
      Arrays.asList(event.getMessageContent().substring(event.getMessageContent().indexOf(' ')).split("\\s+"))
              .stream()
              .filter(Misc::containsUrl)
              .map(String::trim)
              .forEach(url -> {
                if (guild.getBlockedUrls().remove(url)) unblockedUrls.add(url);
              });
      
      if(event.getChannel().canYouManageMessages()) event.deleteMessage();
      dbManager.upsert(guild);
      
      if(unblockedUrls.size() > 0 && event.getChannel().canYouWrite()) {
        StringBuilder msg = new StringBuilder();
        unblockedUrls.forEach(url -> msg.append("- `" + url + "`\n"));
        event.getChannel()
          .sendMessage("Removed the following URL\\s from the list:\n" + msg)
          .exceptionally(ExceptionLogger.get());       
      } 
    }
  }
}
