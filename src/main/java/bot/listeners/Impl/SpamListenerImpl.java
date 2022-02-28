package bot.listeners.Impl;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.SpamListener;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class SpamListenerImpl implements SpamListener {
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
        if(event.getMessageAuthor().asUser().isPresent()) {
          if(Misc.isUserAllowed(event, event.getApi())) return;
        }
        
        // guild isn't restricted
        if(!guild.getRestricted()) return;
        
        // didn't tag everyone
        if (!event.getMessage().mentionsEveryone()) return;

        // message contains a URL / Embed
        if (!event.getMessage().getEmbeds().isEmpty() || Misc.containsUrl(event.getMessageContent())) {
          event.deleteMessage().exceptionally(e -> {
            loggerWrapper.log(Loglevel.ERROR, "failed to delete a message from " + event.getChannel().getId()
                    + " server " + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + e.getMessage());
            return null;
          });

          loggerWrapper.log(Loglevel.WARN, "detected spam message for server " + guild.getId() + " in channel "
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
      }); //guild.ifPresent
    }
  }
}
