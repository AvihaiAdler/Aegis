package bot.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;
import bot.dal.DBManager;
import bot.util.Misc;

public class UnrestrictListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(UnrestrictListener.class);
  private DBManager dbManager;
  
  public UnrestrictListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {    
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      // user doesn't have permission for this command
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      logger.info("invoking " + this.getClass().getName() + " for server " + guild.getId());
      
      if(guild.getRestricted()) {
        guild.setRestricted(false);
        dbManager.upsert(guild);
        
        logger.info("the server " + guild.getId() + " is no longer restricted");
        
        // feedback
        new MessageBuilder().setContent("The server is no longer in restrict mode")
                .send(event.getChannel())
                .exceptionally(ExceptionLogger.get());   
      }
    }); // event.getMessageAuthor().asUser().ifPresent
  }
}
