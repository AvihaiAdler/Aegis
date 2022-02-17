package bot.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import bot.dal.DBManager;
import bot.data.GuildEntity;
import bot.util.Misc;

public class SuspiciousWordsListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public SuspiciousWordsListener(DBManager mongoClient, DiscordApi discordApi) {
    this.dbManager = mongoClient;
    this.discordApi = discordApi;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if (event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
      if(event.getMessageAuthor().asUser().isPresent()) {
        if(Misc.isAllowed(event, discordApi)) return;
      }

      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      
      if(guild == null) return;
      
      // check embeds if there're any
      if(!event.getMessage().getEmbeds().isEmpty()) {
        event.getMessage().getEmbeds().forEach(embed -> {
          if (isSuspicious(embed, guild) && event.getChannel().canYouManageMessages())
            event.deleteMessage();
        });        
      }
      
      // check message content
      var suspiciousContent = checkString(event.getMessageContent(), guild);
      if(suspiciousContent && event.getChannel().canYouManageMessages()) event.deleteMessage();
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
