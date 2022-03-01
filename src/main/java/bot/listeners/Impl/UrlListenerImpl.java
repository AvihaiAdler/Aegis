package bot.listeners.Impl;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import bot.data.GuildEntity;
import bot.listeners.UrlListener;
import bot.util.MessageSender;
import bot.util.Misc;

@Component
public class UrlListenerImpl implements UrlListener {
  private Logger logger = LoggerFactory.getLogger(UrlListenerImpl.class);
  private MessageSender messageSender;

  @Autowired
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  @Override
  public void onMessageCreate(MessageCreateEvent event, GuildEntity guild) {
    // no blocked urls for the guild
    if (guild.getBlockedUrls().isEmpty()) return;

    // filter all urls which matches guild.blockedUrl()
    var urls = Misc.getUrls(event.getMessageContent())
            .stream()
            .filter(url -> guild.getBlockedUrls().contains(url))
            .collect(Collectors.toList());

    // no blocked urls detected - bail
    if (urls.isEmpty()) return;
    
    // delete the message
    event.deleteMessage().exceptionally(e -> {
      logger.error("failed to delete a message from " + event.getChannel().getId()
              + " server " + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
      return null;
    });

    logger.warn("detected blocked url for server " + guild.getGuildName() + " (" + guild.getId() + ")" + " in channel "
            + event.getChannel().getIdAsString() + "\noriginal message " + event.getMessageContent());

    // feedback
    var logChannelId = guild.getLogChannelId();
    if (!Misc.channelExists(logChannelId, event.getServer().get())) return;
    
    var now = ZonedDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
    var msg = DateTimeFormatter.ofPattern("dd/MM/uuuu, HH:mm:ss").format(now) + " (UTC): a message from **"
            + event.getMessageAuthor().getDiscriminatedName() + "** `(" + event.getMessageAuthor().getIdAsString()
            + ")` in **#" + event.getServerTextChannel().get().getName()
            + "** was deleted by **" + event.getApi().getYourself().getDiscriminatedName()
            + "**. Reason: ```contains forbidden url```";
    messageSender.send(event.getServer().get().getTextChannelById(logChannelId).get(), msg, guild);
  }
}
