package bot;

import java.io.IOException;
import org.javacord.api.DiscordApiBuilder;
import bot.dal.DBManager;
import bot.data.GuildEntity;
import bot.listeners.SpamListener;
import bot.listeners.SuspiciousWordsListener;
import bot.util.ConfigManager;

/*
 * TODO:
 * db.guilds.drop()
 * add a check whether we already have a GuildEntity in the db before writing to it
 * command listener
 * bot mention listener
 */

public class Main {
  public static void main(final String[] args) throws Exception {
    var properties = ConfigManager.getInstance();

    try {
      properties.populate();
    } catch (IOException e) {
      // log
      // abort
    }

    final DBManager dbManager;
    dbManager = new DBManager(properties.getProperties().get("connectionString"), 
            properties.getProperties().get("db"),
            properties.getProperties().get("collection"));

    var discordApi = new DiscordApiBuilder()
            .setToken(properties.getProperties().get("token"))
            .login()
            .join();
    
    discordApi.addServerLeaveListener(leaveEvent -> {
      var serverId = leaveEvent.getServer().getIdAsString();
      if (dbManager.findGuildById(serverId) != null) {
        dbManager.delete(serverId);
      }
      System.out.println("left " + serverId);
    });

    // Server joined listener
    discordApi.addServerJoinListener(joinEvent -> {
      var serverId = joinEvent.getServer().getIdAsString();
      if (dbManager.findGuildById(serverId) == null) {
        dbManager.insert(new GuildEntity(serverId));
      }
      System.out.println("joined " + dbManager.findGuildById(serverId));
    });
    
    // Suspicious words Listener
    var suspiciousWordsListener = new SuspiciousWordsListener(dbManager);
    discordApi.addMessageCreateListener(suspiciousWordsListener::onMessageCreate);
    
    // Spam listener
    var spamListener = new SpamListener(dbManager, discordApi);
    discordApi.addMessageCreateListener(spamListener::onMessageCreate);
    
    discordApi.addMessageCreateListener(event -> {
      if(event.getServer().isPresent() && !event.getMessageAuthor().isBotUser() && event.getMessage().getContent().equals("!restrict")) {
        var serverId = event.getServer().get().getIdAsString();
        var guild = dbManager.findGuildById(serverId);

        if(guild != null) {
          guild.setRestricted(true);
          dbManager.update(guild);
        }
        event.deleteMessage();
      }
    });
    
    discordApi.addMessageCreateListener(event -> {
      if(event.getServer().isPresent() && !event.getMessageAuthor().isBotUser() && event.getMessage().getContent().equals("!unrestrict")) {
        var serverId = event.getServer().get().getIdAsString();
        var guild = dbManager.findGuildById(serverId);
        if(guild != null) {
          guild.setRestricted(false);
          dbManager.update(guild);
        }
      }
    });
  }
}
