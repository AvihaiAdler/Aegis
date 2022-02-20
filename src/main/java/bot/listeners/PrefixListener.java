package bot.listeners;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class PrefixListener implements MessageCreateListener {
  private DBManager dbManager;
  
  public PrefixListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isAllowed(event, event.getApi())) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var splitted = event.getMessageContent().split(" ");
      if(splitted.length >= 2) {
        guild.setPrefix(splitted[1]);
        dbManager.upsert(guild);
        
        if(event.getChannel().canYouWrite()) {
          event.getChannel().sendMessage("Prefix has been changed to **" + guild.getPrefix() + "**").exceptionally(ExceptionLogger.get());
        }
      }
    }
  }
}
