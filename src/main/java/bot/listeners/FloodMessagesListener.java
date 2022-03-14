package bot.listeners;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import bot.dal.MessagesDao;
import bot.data.GuildEntity;
import bot.data.MessageEntity;
import bot.util.MessageSender;

@Service
public class FloodMessagesListener implements GeneralListener {
  private Logger logger = LoggerFactory.getLogger(FloodMessagesListener.class);
  private MessagesDao messagesDao;
  private MessageSender messageSender;
  private long floodDelayMillis;
  
  @Autowired
  public void setMessagesDao(MessagesDao messagesDao) {
    this.messagesDao = messagesDao;
  }
  
  @Autowired
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }
  
  @Value("${flood.delay:400}")
  public void setFloodDelayMillis(long floodDelayMillis) {
    this.floodDelayMillis = floodDelayMillis;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event, GuildEntity guild) {
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      var now = Instant.now();
      var nowInMillis = now.toEpochMilli();
      // log the time the message was sent
      messagesDao.save(new MessageEntity(event.getChannel().getIdAsString(), nowInMillis, usr.getDiscriminatedName()));        
      
      // find all messages send in a certain period of time
      var messages = messagesDao.findAllByUsernameAndChannelIdAndSentTimeMillisBetween(usr.getDiscriminatedName(), 
              event.getChannel().getIdAsString(), 
              nowInMillis - 3*floodDelayMillis, 
              nowInMillis);
      
      // messages count in the above period of time seems unreasonable
      if(messages.size() >= 3) {
        logger.warn("detected suspicious activity for server " + guild.getId() + " in channel "
                + event.getChannel().getIdAsString() + "\npossible flood attack");
        
        var zonedNow = ZonedDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
        var msg = DateTimeFormatter.ofPattern("dd/MM/uuuu, HH:mm:ss").format(zonedNow) 
                + " (UTC) detected suspicious activity in **" + event.getServerTextChannel().get().getName() 
                + "** (`" + event.getChannel().getId() + "`)"
                + "by **" + usr.getDiscriminatedName() + "**\n"
                + "```possible flood attack```";
        messageSender.send(event.getServer().get().getTextChannelById(guild.getLogChannelId()).get(), msg, guild);
      }
    }); //event.getMessageAuthor().asUser().ifPresent 
  }
}
