package bot.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;
import bot.dal.DBManager;
import bot.util.Misc;

public class ThresholdListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(ThresholdListener.class);
  private DBManager dbManager;
  
  public ThresholdListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {    
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      // user doesn't have permission for this command
      if(!Misc.isUserAllowed(event, event.getApi())) return;

      if(event.getMessageContent().split("\\s+").length < 2) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      logger.info("invoking " + this.getClass().getName() + " for server " + guild.getId());
      
      // process the command
      try {
        var newThreshold = Integer.valueOf(event.getMessageContent().split("\\s+")[1]);
        // threshold must be 0 or higher
        if(newThreshold >= 0) {
          guild.setThreshold(newThreshold);
          dbManager.upsert(guild);
          
          logger.info("the server " + guild.getId() + " changed their prefix to " + guild.getPrefix());
          
          // feedback
          new MessageBuilder().setContent("Threshold is now **" + guild.getThreshold() + "**")
                  .send(event.getChannel())
                  .exceptionally(ExceptionLogger.get());         
        }
      } catch (NumberFormatException e) {
        logger.error(e.getMessage());
      }
    });
  }
}
