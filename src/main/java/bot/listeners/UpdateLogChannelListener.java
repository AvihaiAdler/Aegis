package bot.listeners;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import bot.dal.DBManager;
import bot.util.Misc;

public class UpdateLogChannelListener implements MessageCreateListener {
  private DBManager dbManager;
  
  public UpdateLogChannelListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
   
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isEmpty()) return;
    if(!Misc.isAllowed(event, event.getApi())) return;
    
    var content = event.getMessageContent().split("\\s+");
    if(content.length < 2) return;
    
    var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
    
    if(Misc.channelExists(content[1], event.getServer().get())) {
      guild.setLogChannelId(content[1]);
      dbManager.upsert(guild);
    }
  }

}
