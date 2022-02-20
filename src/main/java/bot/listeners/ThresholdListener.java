package bot.listeners;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class ThresholdListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public ThresholdListener(DBManager dbManager, DiscordApi discordApi) {
    this.dbManager = dbManager;
    this.discordApi = discordApi;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isAllowed(event, discordApi)) return;

      if(event.getMessageContent().split("\\s+").length < 2) return;

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      try {
        var newThreshold = Integer.valueOf(event.getMessageContent().split("\\s+")[1]);
        if(newThreshold >= 0) {
          guild.setThreshold(newThreshold);
          dbManager.upsert(guild);
          
          if(event.getChannel().canYouWrite()) {
            event.getChannel()
              .sendMessage("Threshold is now **" + guild.getThreshold() + "**")
              .exceptionally(ExceptionLogger.get());          
          }
        }
      } catch (NumberFormatException e) {
        //log
      }
    }
  }
}
