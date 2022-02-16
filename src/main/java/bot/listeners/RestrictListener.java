package bot.listeners;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import bot.dal.DBManager;

/*
 * TODO:
 * exceptionally if needed
 */

public class RestrictListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public RestrictListener(DBManager dbManager, DiscordApi discordApi) {
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
              (usrHighestRole.get().compareTo(botHighestRole) <= 0 &&
              !event.getServer().get().isOwner(event.getMessageAuthor().asUser().get()))) return;

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if(!guild.getRestricted()) {
        guild.setRestricted(true);
        dbManager.update(guild);
        if(event.getChannel().canYouWrite()) {
          event.getChannel().sendMessage("the server is now in restrict mode"); //exceptionally        
        }
      }      
    }
  }
}
