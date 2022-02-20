package bot.listeners;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;
import bot.dal.DBManager;
import bot.util.Misc;

/*
 * TODO
 * add exceptionally if needed
 */

public class UnrestrictListener implements MessageCreateListener {
  private DBManager dbManager;
  
  public UnrestrictListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isAllowed(event, event.getApi())) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if(guild.getRestricted()) {
        guild.setRestricted(false);
        dbManager.upsert(guild);
        
        if(event.getChannel().canYouWrite()) {
          event.getChannel()
            .sendMessage("The server is no longer in restrict mode")
            .exceptionally(ExceptionLogger.get());       
        }
      }
    }
  }
}
