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
import bot.listeners.BlockListener;
import bot.listeners.MentionListener;
import bot.listeners.PrefixListener;
import bot.listeners.RestrictListener;
import bot.listeners.SpamListener;
import bot.listeners.SuspectListener;
import bot.listeners.SuspiciousWordsListener;
import bot.listeners.ThresholdListener;
import bot.listeners.UnblockedListener;
import bot.listeners.UnrestrictListener;
import bot.listeners.UnsuspectListener;
import bot.util.ConfigManager;

/*
 * TODO:
 * add logging for each listener
 * db.guilds.drop()
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

    final DBManager dbManager = new DBManager(properties.getProperties().get("connectionString"),
            properties.getProperties().get("db"), 
            properties.getProperties().get("collection"));

    var discordApi = new DiscordApiBuilder()
            .setToken(properties.getProperties().get("token"))
            .login()
            .join();
    
    // Setup commands listeners
    commands.put("restrict", new RestrictListener(dbManager, discordApi));
    commands.put("unrestrict", new UnrestrictListener(dbManager, discordApi));
    commands.put("suspect", new SuspectListener(dbManager, discordApi));
    commands.put("unsuspect", new UnsuspectListener(dbManager, discordApi));
    commands.put("prefix", new PrefixListener(dbManager, discordApi));
    commands.put("block", new BlockListener(dbManager, discordApi));
    commands.put("unblock", new UnblockedListener(dbManager, discordApi));
    commands.put("threshold", new ThresholdListener(dbManager, discordApi));
    
    // Server leave listener
    discordApi.addServerLeaveListener(leaveEvent -> {
      var serverId = leaveEvent.getServer().getIdAsString();
      if (dbManager.findGuildById(serverId) != null) {
        dbManager.delete(serverId);
      }
      System.out.println("left " + serverId); //log
    });

    // Server joined listener
    discordApi.addServerJoinListener(joinEvent -> {
      var serverId = joinEvent.getServer().getIdAsString();
      if (dbManager.findGuildById(serverId) == null) {
        dbManager.insert(new GuildEntity(serverId, joinEvent.getServer().getName()));
      }
      System.out.println("joined " + dbManager.findGuildById(serverId));  //log
    });
    
    // Suspicious words Listener
    var suspiciousWordsListener = new SuspiciousWordsListener(dbManager, discordApi);
    discordApi.addMessageCreateListener(suspiciousWordsListener::onMessageCreate);
    
    // Spam listener
    var spamListener = new SpamListener(dbManager, discordApi);
    discordApi.addMessageCreateListener(spamListener::onMessageCreate);
    
    var mentionListener = new MentionListener(dbManager, discordApi);
    discordApi.addMessageCreateListener(mentionListener::onMessageCreate);
    
    // Commands listeners
    discordApi.addMessageCreateListener(event -> {
      if(event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
        var content = Arrays.asList(event.getMessageContent().split("\\s+"));
        var serverId = event.getServer().get().getIdAsString();
        var guild = dbManager.findGuildById(serverId);
        
        if(guild == null) {
          dbManager.insert(new GuildEntity(serverId, event.getServer().get().getName()));
          return;
        }
         
        if(!event.getMessageContent().startsWith(guild.getPrefix())) return;
        
        var listener = commands.get(content.get(0).split(guild.getPrefix())[1]);
        if(listener != null) {
          listener.onMessageCreate(event);
        }
      }
    });
  }
}
