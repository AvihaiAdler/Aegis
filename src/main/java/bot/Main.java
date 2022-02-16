package bot;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.listener.message.MessageCreateListener;

import bot.dal.DBManager;
import bot.data.GuildEntity;
import bot.listeners.PrefixListener;
import bot.listeners.RestrictListener;
import bot.listeners.SpamListener;
import bot.listeners.SuspectListener;
import bot.listeners.SuspiciousWordsListener;
import bot.listeners.ThresholdListener;
import bot.listeners.UnrestrictListener;
import bot.listeners.UnsuspectListener;
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
    final Map<String, MessageCreateListener> commands = Collections.synchronizedMap(new HashMap<>());    
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
    
    // Setup commands listeners
    commands.put("restrict", new RestrictListener(dbManager, discordApi));
    commands.put("unrestrict", new UnrestrictListener(dbManager, discordApi));
    commands.put("server", null);
    commands.put("suspect", new SuspectListener(dbManager, discordApi));
    commands.put("unsuspect", new UnsuspectListener(dbManager, discordApi));
    commands.put("prefix", new PrefixListener(dbManager, discordApi));
    commands.put("block", null);
    commands.put("unblock", null);
    commands.put("threshold", new ThresholdListener(dbManager, discordApi));
    
    // Server leave listener
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
    
    // Commands listeners
    discordApi.addMessageCreateListener(event -> {
      if(event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
        var content = Arrays.asList(event.getMessageContent().split("\\s+"));
        var serverId = event.getServer().get().getIdAsString();
        var guild = dbManager.findGuildById(serverId);
        
        if(guild == null) {
          dbManager.insert(new GuildEntity(serverId));
          System.out.println("guild is null");
          return;
        }
        
        commands.keySet().forEach(key -> {
          if(content.get(0).toLowerCase().equals(guild.getPrefix() + key)) {
            commands.get(key).onMessageCreate(event);
          }
        });
      }
    });
  }
}
