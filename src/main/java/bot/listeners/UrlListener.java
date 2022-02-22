package bot.listeners;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

public class UrlListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(UrlListener.class);
  private DBManager dbManager;
  
  public UrlListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if (event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if (guild == null)
        return;

      if(event.getMessageAuthor().asUser().isPresent()) {
        if(Misc.isUserAllowed(event, event.getApi())) return;
      }
      
      if(guild.getBlockedUrls().isEmpty()) return;
      
      if(Misc.containsUrl(event.getMessageContent()) && event.getChannel().canYouManageMessages()) {
        // filter all urls which matches guild.blockedUrl()
        final String urlRegex = "<?\\b(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>?";
        var content = Arrays.asList(event.getMessageContent().split("\\s+"))
                .stream()
                .filter(word -> Pattern.matches(urlRegex, word))
                .map(url -> url.startsWith("<") ? url.substring(1) : url)
                .map(url -> url.endsWith(">") ? url.substring(0, url.length()) : url)
                .filter(url -> guild.getBlockedUrls().contains(url))
                .collect(Collectors.toList()); 
        
        if(content.size() > 0) {
          event.deleteMessage().exceptionally(ExceptionLogger.get());
          
          logger.info("detected blocked url for server " + guild.getId() + " in channel " + event.getChannel().getIdAsString() + "\noriginal message " + event.getMessageContent());
          
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
                            + event.getApi().getYourself().getDiscriminatedName() + "**. Reason: ```contains forbidden url```");
          }
        }
      }
    } 
  } 
}