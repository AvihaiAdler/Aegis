package bot.listeners;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class BlockListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(BlockListener.class);
  private DBManager dbManager;
  
  public BlockListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      if(!Misc.isUserAllowed(event, event.getApi())) return;
      if(event.getMessageContent().split("\\s+").length < 2) return;
      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      
      var blockedUrls = new HashSet<String>();
      
      // collect all the urls from the message
      Arrays.asList(event.getMessageContent().substring(event
              .getMessageContent()
              .indexOf(' '))
              .split("\\s+")).stream()
              .filter(Misc::containsUrl).map(String::trim).forEach(url -> {
                if (guild.getBlockedUrls().add(url)) blockedUrls.add(url);
              });
      dbManager.upsert(guild);
      
      // no urls were added - bail
      if (blockedUrls.size() == 0) return;
      
      StringBuilder msg = new StringBuilder();
      blockedUrls.forEach(url -> msg.append("- `" + url + "`\n"));

      logger.info("the server " + guild.getId() + " added the following to their block list:\n" + msg);

      // feedback
      new MessageBuilder().setContent("The following URL\\s have been added to the list:\n" + msg)
                .send(event.getChannel())
                .exceptionally(ExceptionLogger.get())
                .thenRun(() -> event.getMessage().delete()) // delete the command to prevent clutter
                .exceptionally(ExceptionLogger.get());
      
    }); //event.getMessageAuthor().asUser().ifPresent
  }
}
