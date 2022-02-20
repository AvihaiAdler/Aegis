package bot.listeners;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import bot.dal.DBManager;
import bot.util.Misc;

public class UpdateLogChannelListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public UpdateLogChannelListener(DBManager dbManager, DiscordApi discordApi) {
    this.dbManager = dbManager;
    this.discordApi = discordApi;
  }
   
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isEmpty()) return;
    if(!Misc.isAllowed(event, discordApi)) return;
    
    var content = event.getMessageContent().split("\\s+");
    if(content.length < 2) return;
    
    var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
    
    if(Misc.channelExists(content[1], event)) {
      guild.setLogChannelId(content[1]);
      dbManager.upsert(guild);
    }
  }

}
