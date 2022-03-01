package bot.listeners.Impl;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.data.GuildEntity;
import bot.listeners.SpamListener;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class SpamListenerImpl implements SpamListener {
  private Logger logger = LoggerFactory.getLogger(UrlListenerImpl.class);
  private MessageSender messageSender;
  
  @Autowired
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event, GuildEntity guild) { 
    // guild isn't restricted
    if(!guild.getRestricted()) return;
    
    // didn't tag everyone
    if (!event.getMessage().mentionsEveryone()) return;

    // message contains a URL / Embed
    if (!event.getMessage().getEmbeds().isEmpty() || Misc.containsUrl(event.getMessageContent())) {
      event.deleteMessage().exceptionally(e -> {
        logger.error("failed to delete a message from " + event.getChannel().getId()
                + " server " + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
        return null;
      });

      logger.warn("detected spam message for server " + guild.getId() + " in channel "
              + event.getChannel().getIdAsString() + "\noriginal message " + event.getMessageContent());

      // log to the log channel
      var logChannelId = guild.getLogChannelId();

      // logging channel doesn't exists
      if (!Misc.channelExists(logChannelId, event.getServer().get())) return;
      
      // log the delete action
      var now = ZonedDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
      var message = DateTimeFormatter.ofPattern("dd/MM/uuuu, HH:mm:ss").format(now) + " (UTC): a message from **"
              + event.getMessageAuthor().getDiscriminatedName() + "** (`" + event.getMessageAuthor().getIdAsString() + "`)" 
              + " in **#" + event.getServerTextChannel().get().getName() + "** (`" + event.getChannel().getId()
              + "`) was deleted by **" + event.getApi().getYourself().getDiscriminatedName()
              + "**. Reason: ```possible spam```";
      messageSender.send(event.getServer().get().getTextChannelById(logChannelId).get(), message, guild);
    }   
  }
}
