package bot.listeners;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class UrlListenerImpl implements UrlListener {
  private LoggerWrapper loggerWrapper;
  private GuildDao guildDao;
  private MessageSender messageSender;

  @Autowired
  public void setLoggerWrapper(LoggerWrapper loggerWrapper) {
    this.loggerWrapper = loggerWrapper;
  }

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
          loggerWrapper.log(Loglevel.ERROR, "failed to delete a message from " + event.getChannel().getId()
                  + " server " + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + e.getMessage());
          return null;
        });

        loggerWrapper.log(Loglevel.WARN, "detected blocked url for server " + guild.getGuildName() + " (" + guild.getId() + ")" + " in channel "
                + event.getChannel().getIdAsString() + "\noriginal message " + event.getMessageContent());

        // feedback
        if (!Misc.channelExists(guild.getLogChannelId(), event.getServer().get())) return;
        
        var now = ZonedDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
        var msg = DateTimeFormatter.ofPattern("dd/MM/uuuu, HH:mm:ss").format(now) + " (UTC): a message from **"
                + event.getMessageAuthor().getDiscriminatedName() + "** `(" + event.getMessageAuthor().getIdAsString()
                + ")` in **#" + event.getServerTextChannel().get().getName()
                + "** was deleted by **" + event.getApi().getYourself().getDiscriminatedName()
                + "**. Reason: ```contains forbidden url```";
        messageSender.send(event.getChannel(), msg, guild);
      }); // guild.ifPresent
    }
  }
}
