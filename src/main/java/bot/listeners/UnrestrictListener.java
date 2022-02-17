package bot.listeners;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import bot.dal.DBManager;
import bot.util.Misc;

/*
 * TODO
 * add exceptionally if needed
 */

public class UnrestrictListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public UnrestrictListener(DBManager dbManager, DiscordApi discordApi) {
    this.dbManager = dbManager;
    this.discordApi = discordApi;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isAllowed(event, discordApi)) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if(guild.getRestricted()) {
        guild.setRestricted(false);
        dbManager.update(guild);
        
        if(event.getChannel().canYouWrite()) {
          event.getChannel().sendMessage("the server is no longer in restrict mode"); //exceptionally          
        }
      }
    }
  }
}
