package bot.listeners;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import bot.dal.DBManager;
import bot.util.Misc;

public class PrefixListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public PrefixListener(DBManager dbManager, DiscordApi discordApi) {
    this.dbManager = dbManager;
    this.discordApi = discordApi;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isAllowed(event, discordApi)) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var splitted = event.getMessageContent().split(" ");
      if(splitted.length >= 2) {
        guild.setPrefix(splitted[1]);
        dbManager.update(guild);
        
        if(event.getChannel().canYouWrite()) {
          event.getChannel().sendMessage("prefix is now **" + guild.getPrefix() + "**");
        }
      }
    }
  }
}
