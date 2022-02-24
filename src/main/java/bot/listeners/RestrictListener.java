package bot.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class RestrictListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(RestrictListener.class);
  private DBManager dbManager;
  
  public RestrictListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      logger.info("invoking " + this.getClass().getName() + " for server " + guild.getId());
      
      if(!guild.getRestricted()) {
        guild.setRestricted(true);
        dbManager.upsert(guild);
        if(event.getChannel().canYouWrite()) {
          logger.info("the server " + guild.getId() + " is now restricted");
          
          new MessageBuilder().setContent("The server is now in restrict mode")
                  .send(event.getChannel())
                  .exceptionally(ExceptionLogger.get());             
        }
      }      
    }
  }
}
