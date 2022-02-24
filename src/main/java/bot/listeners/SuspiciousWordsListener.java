package bot.listeners;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.data.GuildEntity;
import bot.util.Misc;

public class SuspiciousWordsListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(SuspiciousWordsListener.class);
  private DBManager dbManager;
  
  public SuspiciousWordsListener(DBManager mongoClient) {
    this.dbManager = mongoClient;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if (event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
      if(event.getMessageAuthor().asUser().isPresent()) {
        if(Misc.isUserAllowed(event, event.getApi())) return;
      }

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      
      if(guild == null) return;
      
      logger.info("invoking " + this.getClass().getName() + " for server " + guild.getId());
      
      // check embeds if there're any
      if(!event.getMessage().getEmbeds().isEmpty()) {
        event.getMessage().getEmbeds().forEach(embed -> {
          if (isSuspicious(embed, guild) && event.getChannel().canYouManageMessages()) {
            event.deleteMessage().exceptionally(ExceptionLogger.get());
            log(guild, event);
            return;            
          }
        });        
      }
      
      // check message content
      var suspiciousContent = checkString(event.getMessageContent(), guild);
      if(suspiciousContent && event.getChannel().canYouManageMessages()) {
        event.deleteMessage().exceptionally(ExceptionLogger.get());
        log(guild, event);
        return;
      }
    }
  }
  
  private void log(GuildEntity guild, MessageCreateEvent event) {
    logger.info("detected some suspicious words for " + guild.getId() + " in channel " + event.getChannel().getIdAsString() + "\noriginal message " + event.getMessageContent());
    
    // log to the log channel
    var logChannelId = guild.getLogChannelId();
    if(logChannelId == null) return;
    if(Misc.channelExists(logChannelId, event.getServer().get()) && Misc.canLog(logChannelId, event)) {
      var now = ZonedDateTime.now(ZoneId.of(ZoneOffset.UTC.toString()));
      
      new MessageBuilder().setContent(DateTimeFormatter.ofPattern("dd/MM/uuuu, HH:mm:ss").format(now)
              + " (UTC): a message from **" + event.getMessageAuthor().getDiscriminatedName() + "** `("
              + event.getMessageAuthor().getIdAsString() + ")` was deleted by **"
              + event.getApi().getYourself().getDiscriminatedName() + "**. Reason: ```suspicious words were detected```")
            .send(event.getServer().get().getTextChannelById(logChannelId).get())
            .exceptionally(ExceptionLogger.get());
    }
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
