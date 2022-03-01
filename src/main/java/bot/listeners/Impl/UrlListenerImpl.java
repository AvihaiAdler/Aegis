package bot.listeners.Impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.UrlListener;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class UrlListenerImpl implements UrlListener {
  private Logger logger = LogManager.getLogger();
  private GuildDao guildDao;
  private MessageSender messageSender;

  @Autowired
  public void setGuildDao(GuildDao guildDao) {
    this.guildDao = guildDao;
  }

  @Autowired
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if (event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        // user is allowed - ignore their message
        if (event.getMessageAuthor().asUser().isPresent()) {
          if (Misc.isUserAllowed(event, event.getApi()))
            return;
        }

        // no blocked urls for the guild
        if (guild.getBlockedUrls().isEmpty()) return;

        // filter all urls which matches guild.blockedUrl()
        final String urlRegex = "<?\\b(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>?";
        var content = Arrays.asList(event.getMessageContent().split("\\s+")).stream()
                .filter(word -> Pattern.matches(urlRegex, word))
                .map(url -> url.startsWith("<") ? url.substring(1) : url)
                .map(url -> url.endsWith(">") ? url.substring(0, url.length()) : url)
                .filter(url -> guild.getBlockedUrls().contains(url)).collect(Collectors.toList());

        // no blocked urls detected - bail
        if (content.size() == 0) return;
        
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
      }); // guild.ifPresent
    }
  }
}