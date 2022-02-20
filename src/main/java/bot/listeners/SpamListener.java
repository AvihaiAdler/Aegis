package bot.listeners;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class SpamListener implements MessageCreateListener {
  private DBManager dbManager;
  
  public SpamListener(DBManager mongoClient) {
    this.dbManager = mongoClient;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if (event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if (guild == null)
        return;

      if(event.getMessageAuthor().asUser().isPresent()) {
        if(Misc.isAllowed(event, event.getApi())) return;
      }
      
      // if the guild is 'restricted' 
      if (guild.getRestricted() && event.getMessage().mentionsEveryone()) {
        if(!event.getMessage().getEmbeds().isEmpty() || Misc.containsUrl(event.getMessageContent()) && event.getChannel().canYouManageMessages()) {
          event.deleteMessage().exceptionally(ExceptionLogger.get());
          
          // log to the log channel
          var logChannelId = guild.getLogChannelId();
          if(logChannelId == null) return;
          if(Misc.channelExists(logChannelId, event.getServer().get()) && Misc.canLog(logChannelId, event)) {
            var now = ZonedDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
            event.getServer().get()
              .getChannelById(logChannelId).get()
              .asServerTextChannel().get()
                    .sendMessage(DateTimeFormatter.ofPattern("dd/MM/uuuu, HH:mm:ss").format(now)
                            + " (UTC): a message from **" + event.getMessageAuthor().getDiscriminatedName() + "** `("
                            + event.getMessageAuthor().getIdAsString() + ")` was deleted by **"
                            + event.getApi().getYourself().getDiscriminatedName() + "**. Reason: ```possible spam```");
          }
        }
      }
    }
  }
}
