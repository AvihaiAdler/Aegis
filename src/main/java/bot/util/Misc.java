package bot.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import bot.data.GuildEntity;

public class Misc {
  public static boolean containsUrl(String potentialUrl) {
    final String urlRegex = "<?\\b(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>?";
    if(potentialUrl == null)
      return false;
    var content = Arrays.asList(potentialUrl.split("\\s+"))
            .stream()
            .filter(word -> Pattern.matches(urlRegex, word))
            .collect(Collectors.toList()); 
    return !content.isEmpty();
  }
  
  public static boolean isAllowed(MessageCreateEvent event, DiscordApi discordApi) {
    var usrHighestRole = event.getServer().get().getHighestRole(event.getMessageAuthor().asUser().get());
    var botHighestRole = event.getServer().get().getHighestRole(discordApi.getYourself()).get();
    
    // user has no role
    if(!usrHighestRole.isPresent()) {
      return false;
    }
    
    // user highest role is lower than the bot && use isn't the server owner
    if(usrHighestRole.get().compareTo(botHighestRole) <= 0 && !event.getServer().get().isOwner(event.getMessageAuthor().asUser().get())) {
      return false;
    }

    return true;
  }
  
  public static List<EmbedBuilder> getInfo(GuildEntity guild) {
    var info = new ArrayList<EmbedBuilder>();
    
    var embed = new EmbedBuilder()
            .setTitle("Server: " + guild.getId())
            .addField("Prefix", guild.getPrefix())
            .addField("Threshold", Integer.toString(guild.getThreshold()))
            .addField("Restricted", guild.getRestricted() ? "Yes" : "No");
    
    info.add(embed);
    
    suspiciousWords(guild).forEach(str -> info.add(new EmbedBuilder().setTitle("Suspicious words:").setDescription(str)));
    blockedUrls(guild).forEach(urls -> info.add(new EmbedBuilder().setTitle("Blocked urls:").setDescription(urls)));
    return info;
  }
  
  public static List<String> suspiciousWords(GuildEntity guild) {
    var wordLst = new ArrayList<String>();

    var words = new StringBuilder();
    
    var newLine = 0;
    for(var word : guild.getSuspiciousWords()) {
      if(newLine % 5 == 4) {
        words.delete(words.length()-1, words.length());
        words.append("\n");
      }
      
      if(words.length() + (word + ", ").length() >= 4096) {
        words.delete(words.length()-2, words.length());
        wordLst.add(words.toString());
        words.delete(0, words.length());
      }
      
      words.append(word + ", ");
      newLine++;
    }
    
    if(words.length() > 3) {
      words.delete(words.length()-2, words.length());
      wordLst.add(words.toString());      
    }
    return wordLst;
  }
  
  public static List<String> blockedUrls(GuildEntity guild) {
    var blocked = new ArrayList<String>();

    var urls = new StringBuilder();
    
    for(var url : guild.getBlockedUrls()) {    
      if(urls.length() + (url + "\n").length() >= 4096) {
        urls.delete(urls.length()-1, urls.length());
        blocked.add(urls.toString());
        urls.delete(0, urls.length());
      }
      
      urls.append(url + "\n");
    }
    
    if(urls.length() > 2) {
      urls.delete(urls.length()-1, urls.length());
      blocked.add(urls.toString());      
    }
    return blocked;
  }
}
