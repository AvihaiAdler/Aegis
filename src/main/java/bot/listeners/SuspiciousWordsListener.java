package bot.listeners;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bot.dal.SpamUrlsDao;
import bot.data.GuildEntity;
import bot.data.SpamUrlEntity;
import bot.util.MessageSender;
import bot.util.Misc;

@Component
public class SuspiciousWordsListener implements GeneralListener {
  private Logger logger = LoggerFactory.getLogger(SuspiciousWordsListener.class);
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
    // check embeds if there're any
    if(!event.getMessage().getEmbeds().isEmpty()) {
      event.getMessage().getEmbeds().forEach(embed -> {
        if (isSuspicious(embed, guild)) {
          
          event.deleteMessage().exceptionally(e -> {
            logger.error("failed to delete a message from " + event.getChannel().getId()
                    + " server " + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
            return null;
          });
          
          // save the urls into the db
          saveMessageUrls(event.getMessageContent(), event.getServer().get().getIdAsString());
          log(guild, event);  // feedback
          return;            
        }
      });        
    }
    
    // check message content
    if (checkString(event.getMessageContent(), guild)) {
      
      event.deleteMessage().exceptionally(e -> {
        logger.error("failed to delete a message from " + event.getChannel().getId()
                + " server " + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
        return null;
      });
      
      // save the urls into the db
      saveMessageUrls(event.getMessageContent(), event.getServer().get().getIdAsString());
      log(guild, event); // feedback
      return;
    }
  }
  
  private void saveMessageUrls(String message, String serverId) {
    Misc.getUrls(message).forEach(url -> {
      spamUrlsDao.findOneByUrl(url).ifPresentOrElse(spamEntity -> { // url in the cache
        
        // add the server id into the list of servers the url was sent it
        spamEntity.getServers().add(serverId);
        spamUrlsDao.save(spamEntity);
      }, () -> {  // url insn't the cache
        spamUrlsDao.save(new SpamUrlEntity(Instant.now().getEpochSecond(), url, serverId));
      });  
    });
  }
  
  private void log(GuildEntity guild, MessageCreateEvent event) {
    logger.warn("detected some suspicious words for "  + guild.getGuildName() + " (" + guild.getId() + ")" + " in channel " + event.getChannel().getIdAsString() + "\noriginal message " + event.getMessageContent());
    
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
            + "**. Reason: ```possible spam: message contains suspicious words```";
    messageSender.send(event.getServer().get().getTextChannelById(logChannelId).get(), message, guild);
  }
  
  private boolean isSuspicious(Embed embed, GuildEntity guild) {
    var sus = false;
    if(embed.getTitle().isPresent())
      sus |= checkString(embed.getTitle().get(), guild);
    
    if(embed.getDescription().isPresent())
      sus |= checkString(embed.getDescription().get(), guild);
    
    return sus;
  }
  
  /*
   * count the number of suspicious words in a string and compare it to the threshold
   */
  private boolean checkString(String str, GuildEntity guild) {
    if(guild.getThreshold() == 0) return false;
    
    List<Integer> scores = new ArrayList<>();
    Arrays.asList(convertUnicode(str).split("\\s+")).forEach(word -> {
      if(guild.getSuspiciousWords() != null && guild.getSuspiciousWords().contains(word))
        scores.add(1);
    });

    int wordsCount = scores.stream().reduce(0, (a, b) -> a+b);
    if(wordsCount >= guild.getThreshold())
      return true;
    return false;
}
  
  private String convertUnicode(String searchText) {
    return searchText.toLowerCase()
            .replaceAll("[áàäâãаạąą]", "a")
            .replaceAll("[сƈċ]", "c")
            .replaceAll("[ԁɗ]", "d")
            .replaceAll("[еẹėéèëê]", "e")
            .replaceAll("ġ", "g")
            .replaceAll("һ", "h")
            .replaceAll("[іíïìî]", "i")
            .replaceAll("[јʝ]", "j")
            .replaceAll("κ", "k")
            .replaceAll("[ӏḷ]", "l")
            .replaceAll("ո", "n")
            .replaceAll("[оοօȯọỏơóòöôõ]", "o")
            .replaceAll("р", "p")
            .replaceAll("զ", "q")
            .replaceAll("ʂ", "s")
            .replaceAll("[υսüúùû]", "u")
            .replaceAll("[νѵ]", "v")
            .replaceAll("[хҳ]", "x")
            .replaceAll("[уý]", "y")
            .replaceAll("[ʐż]", "z");
  }
}
