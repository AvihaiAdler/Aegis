package bot.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import bot.data.GuildEntity;

public class Misc {
  public static boolean containsUrl(String message) {
    return !getUrls(message).isEmpty();
  }
  
  public static Set<String> getUrls(String message) {
    final String urlRegex = "\\b(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$";

    return Arrays.asList(message.split("\\s+"))
        .stream()
        .map(url -> url.startsWith("<") ? url.substring(1) : url)
        .map(url -> url.endsWith(">") ? url.substring(0, url.length()) : url)
        .filter(word -> {
          System.err.println(word + " matches? " + Pattern.matches(urlRegex, word));
          return Pattern.matches(urlRegex, word);
        })
        .collect(Collectors.toSet());
  }

  public static boolean isUserAllowed(MessageCreateEvent event) {
    var usrHighestRole = event.getServer().get().getHighestRole(event.getMessageAuthor().asUser().get());

    // user has no role
    if (!usrHighestRole.isPresent()) {
      return false;
    }

    // if the user isn't the server owner and isn't an ADMINISTRATOR
    if (!usrHighestRole.get().getAllowedPermissions().contains(PermissionType.ADMINISTRATOR)
        && !event.getServer().get().isOwner(event.getMessageAuthor().asUser().get())) {
      return false;
    }

    return true;
  }

  public static boolean channelExists(String channelId, Server server) {
    var channelsId = server
        .getChannels()
        .stream()
        .map(channel -> channel.getIdAsString())
        .collect(Collectors.toList());
    if (channelsId.contains(channelId))
      return true;
    return false;
  }

  public static List<EmbedBuilder> getInfo(GuildEntity guild, Server server) {
    var info = new ArrayList<EmbedBuilder>();

    var embed = new EmbedBuilder()
        .setTitle("Info")
        .addField("Prefix", guild.getPrefix())
        .addField("Threshold", Integer.toString(guild.getThreshold()))
        .addField("Restricted", guild.getRestricted() ? "Yes" : "No");

    if (guild.getLogChannelId() != null && channelExists(guild.getLogChannelId(), server)) {
      embed.addInlineField("Logging channel", guild.getLogChannelId());
      embed.addInlineField("Name", server.getChannelById(guild.getLogChannelId()).get().getName());
    }

    var suspiciousWords = suspiciousWords(guild);
    var blockedUrls = blockedUrls(guild);
    if (suspiciousWords.size() <= 3) {
      suspiciousWords.forEach(str -> embed.addField("Suspicious words", str));
      info.add(embed);
    } else {
      info.add(embed);
      var susEmbed = new EmbedBuilder().setTitle("Suspicious words");

      var counter = 0;
      for (var str : suspiciousWords) {
        if (counter != 0 && counter % 5 == 0) {
          info.add(susEmbed);
          susEmbed = new EmbedBuilder().setTitle("Info:");
        }
        susEmbed.addField(Integer.toString((counter % 5) + 1), str);
        counter++;

      }
    }

    blockedUrls.forEach(urls -> info.add(new EmbedBuilder().setTitle("Blocked urls:").setDescription(urls)));

    return info;
  }

  public static List<String> suspiciousWords(GuildEntity guild) {
    var wordLst = new ArrayList<String>();

    var words = new StringBuilder();

    var newLine = 0;
    for (var word : guild.getSuspiciousWords()) {
      if (newLine % 5 == 4) {
        words.delete(words.length() - 1, words.length());
        words.append("\n");
      }

      if (words.length() + (word + ", ").length() >= 1024) {
        words.delete(words.length() - 2, words.length());
        wordLst.add(words.toString());
        words.delete(0, words.length());
      }

      words.append(word + ", ");
      newLine++;
    }

    if (words.length() > 3) {
      words.delete(words.length() - 2, words.length());
      wordLst.add(words.toString());
    }
    return wordLst;
  }

  public static List<String> blockedUrls(GuildEntity guild) {
    var blocked = new ArrayList<String>();

    var urls = new StringBuilder();

    for (var url : guild.getBlockedUrls()) {
      if (urls.length() + (url + "\n\n").length() >= 4024) {
        urls.delete(urls.length() - 1, urls.length());
        blocked.add(urls.toString());
        urls.delete(0, urls.length());
      }

      urls.append(url + "\n\n");
    }

    if (urls.length() > 2) {
      urls.delete(urls.length() - 1, urls.length());
      blocked.add(urls.toString());
    }
    return blocked;
  }
  
  public static String parseThrowable(Throwable throwable) {
    return Arrays.asList(throwable.getStackTrace()).stream().map(Object::toString).collect(Collectors.joining("\n"));  
  }
}
