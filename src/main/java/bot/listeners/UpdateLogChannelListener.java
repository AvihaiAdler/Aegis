package bot.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class UpdateLogChannelListener implements MessageCreateListener {
  private Logger logger = LogManager.getLogger(UpdateLogChannelListener.class);
  private DBManager dbManager;

  public UpdateLogChannelListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }

  @Override
  public void onMessageCreate(MessageCreateEvent event) {    
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      // user doesn't have permission for this command
      if (!Misc.isUserAllowed(event, event.getApi())) return;
      
      var content = event.getMessageContent().split("\\s+");
      if (content.length < 2) return;

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      
      // change the channel
      if (Misc.channelExists(content[1], event.getServer().get())) {
        guild.setLogChannelId(content[1]);
        dbManager.upsert(guild);

        logger.info("the server " + guild.getId() + " changed their logging channel to " + guild.getLogChannelId());
        
        // feedback
        new MessageBuilder().setContent("Logs will appear at **#" + event.getServer()
                    .get()
                    .getChannelById(guild.getLogChannelId())
                    .get()
                    .getName() + "**")
                .send(event.getChannel())
                .exceptionally(ExceptionLogger.get());
      }
    }); // event.getMessageAuthor().asUser().ifPresent
  }

}
