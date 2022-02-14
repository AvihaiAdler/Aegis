package bot.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import bot.dal.DBManager;
import bot.data.GuildEntity;

public class SuspiciousWordsListener implements MessageCreateListener {
  private DBManager dbManager;
  
  public SuspiciousWordsListener(DBManager mongoClient) {
    this.dbManager = mongoClient;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if (event.getServer().isPresent() && !event.getMessageAuthor().isBotUser()
            && !event.getMessageAttachments().isEmpty()) {
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      event.getMessage().getEmbeds().forEach(embed -> {
        if (isSuspicious(embed, guild))
          event.deleteMessage();
      });
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
  
  private boolean checkString(String str, GuildEntity guild) {
    List<Integer> scores = new ArrayList<>();
    Arrays.asList(convertUnicode(str).split("\\s+")).forEach(word -> {
      if(guild.getSuspiciousWords() != null && guild.getSuspiciousWords().get(word) != null)
        scores.add(guild.getSuspiciousWords().get(word));
    });

    int weight = scores.stream().reduce(0, (a, b) -> a+b);
    if(weight >= guild.getThreshold())
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
