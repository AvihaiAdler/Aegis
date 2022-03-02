package bot.listeners.Impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.SpamUrlsDao;
import bot.data.GuildEntity;
import bot.data.SpamUrlEntity;
import bot.listeners.EveryoneListener;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class EveyoneListenerImpl implements EveryoneListener {
  private Logger logger = LoggerFactory.getLogger(EveyoneListenerImpl.class);
  private MessageSender messageSender;
  private SpamUrlsDao spamUrlsDao;
  
  @Autowired
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }
  
  @Autowired
  public void setSpamUrlsDao(SpamUrlsDao spamUrlsDao) {
    this.spamUrlsDao = spamUrlsDao;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event, GuildEntity guild) { 
    // guild isn't restricted
    if(!guild.getRestricted()) return;
    
    // tag everyone
    if (event.getMessageContent().contains("@everyone") || event.getMessage().mentionsEveryone()) {

      // message contains a URL / Embed
      if (!event.getMessage().getEmbeds().isEmpty() || Misc.containsUrl(event.getMessageContent())) {
        event.deleteMessage().exceptionally(e -> {
          logger.error("failed to delete a message from " + event.getChannel().getId()
                  + " server " + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
          return null;
        });
        
        //save the potential spam message in the db
        Misc.getUrls(event.getMessageContent()).forEach(url -> {
          
          spamUrlsDao.findOneByUrl(url).ifPresentOrElse(spamEntity -> { // url in the cache
            
            // add the server id into the list of servers the url was sent it
            spamEntity.getServers().add(event.getServer().get().getIdAsString());
            spamUrlsDao.save(spamEntity);
          }, () -> {  // url insn't the cache
            spamUrlsDao.save(new SpamUrlEntity(Instant.now().getEpochSecond(), url, event.getServer().get().getIdAsString()));
          });      
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
}
