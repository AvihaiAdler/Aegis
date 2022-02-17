package bot.listeners;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import bot.dal.DBManager;
import bot.util.Misc;

public class SpamListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public SpamListener(DBManager mongoClient, DiscordApi discordApi) {
    this.dbManager = mongoClient;
    this.discordApi = discordApi;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if (event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if (guild == null)
        return;

      if(event.getMessageAuthor().asUser().isPresent()) {
//        var usrHighestRole = event.getServer().get().getHighestRole(event.getMessageAuthor().asUser().get());
//        var botHighestRole = event.getServer().get().getHighestRole(discordApi.getYourself()).get();
//        
//        // check if the user has higher role than the bot  
//        if(usrHighestRole.isPresent() && usrHighestRole.get().compareTo(botHighestRole) > 0) return;
//
//        //check if the user is the server owner
//        if(event.getServer().get().isOwner(event.getMessageAuthor().asUser().get())) return;
        if(Misc.isAllowed(event, discordApi)) return;
      }
      
      // if the guild is 'restricted' 
      if (guild.getRestricted() && event.getMessage().mentionsEveryone()) {
        if(!event.getMessage().getEmbeds().isEmpty() || Misc.containsUrl(event.getMessageContent()) && event.getChannel().canYouManageMessages()) {
          event.deleteMessage();
        }
      }
    }
  }
}
