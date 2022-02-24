package bot.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class PrefixListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(PrefixListener.class);
  private DBManager dbManager;
  
  public PrefixListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {    
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      // user doesn't have permissions for this command
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      
      // process message content
      var splitted = event.getMessageContent().split(" ");
      
      if(splitted.length >= 2) {
        guild.setPrefix(splitted[1]);
        dbManager.upsert(guild);
        
        // feedback
        new MessageBuilder().setContent("Prefix has been changed to **" + guild.getPrefix() + "**")
                .send(event.getChannel())
                .exceptionally(ExceptionLogger.get());
        
        logger.info("prefix for " + guild.getId() + ":" + guild.getGuildName() + " changed to " + guild.getPrefix());
      }
    }); //event.getMessageAuthor().asUser().ifPresent
  }
}
