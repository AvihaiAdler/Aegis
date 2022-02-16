package bot.listeners;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;

public class MentionListener implements MessageCreateListener {
  private DBManager dbManager;
  private DiscordApi discordApi;
  
  public MentionListener(DBManager dbManager, DiscordApi discordApi) {
    this.dbManager = dbManager;
    this.discordApi = discordApi;
  }

  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent() && event.getMessage().getMentionedUsers().contains(discordApi.getYourself())) {
      var usrHighestRole = event.getServer().get().getHighestRole(event.getMessageAuthor().asUser().get());
      var botHighestRole = event.getServer().get().getHighestRole(discordApi.getYourself()).get();
      
      // if the user has a lower role than the bot & isn't the server owner - return
      if(!usrHighestRole.isPresent() || 
              (usrHighestRole.get().compareTo(botHighestRole) <= 0 &&
              !event.getServer().get().isOwner(event.getMessageAuthor().asUser().get()))) return;

      
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      var embed = new EmbedBuilder()
              .setTitle("Server: " + guild.getGuildName() + "\t" + guild.getId())
              .addInlineField("Prefix", guild.getPrefix())
              .addInlineField("Threshold", Integer.toString(guild.getThreshold()))
              .addInlineField("Restricted", Boolean.toString(guild.getRestricted()));
      
      if(event.getChannel().canYouWrite()) event.getChannel().sendMessage(embed).exceptionally(ExceptionLogger.get());
    }
  }
}
