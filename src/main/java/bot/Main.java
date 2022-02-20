package bot;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.intent.Intent;
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
import bot.listeners.UpdateLogChannelListener;
import bot.util.ConfigManager;

/*
 * TODO:
 * add logging for each listener
 */

public class Main {
  public static void main(final String[] args) throws Exception {
    final Map<String, MessageCreateListener> commands = Collections.synchronizedMap(new HashMap<>());    
    var properties = ConfigManager.getInstance();

    try {
      properties.populate();
    } catch (IOException e) {
      // log
      System.exit(1);
    } catch (NullPointerException ne) {
      //log
      System.exit(1);
    }

    final DBManager dbManager = new DBManager();

    dbManager.initConnection(properties.getProperties().get("connectionString"), 
            properties.getProperties().get("db"),
            properties.getProperties().get("collection"));
    
    var discordApi = new DiscordApiBuilder()
            .setToken(properties.getProperties().get("token"))
            .setAllNonPrivilegedIntentsExcept(Intent.DIRECT_MESSAGES, 
                    Intent.DIRECT_MESSAGE_TYPING, 
                    Intent.DIRECT_MESSAGE_REACTIONS,
                    Intent.GUILD_INVITES,
                    Intent.GUILD_WEBHOOKS,
                    Intent.GUILD_MESSAGE_TYPING)
            .login()
            .join();
    
    discordApi.setMessageCacheSize(30, 60*10); //store only 30 messages per channel for 10 minutes
    discordApi.updateActivity(ActivityType.WATCHING, "messages");
    
    // Commands listeners
    commands.put("restrict", new RestrictListener(dbManager));
    commands.put("unrestrict", new UnrestrictListener(dbManager));
    commands.put("suspect", new SuspectListener(dbManager));
    commands.put("unsuspect", new UnsuspectListener(dbManager));
    commands.put("prefix", new PrefixListener(dbManager));
    commands.put("block", new BlockListener(dbManager));
    commands.put("unblock", new UnblockedListener(dbManager));
    commands.put("threshold", new ThresholdListener(dbManager));
    commands.put("logto", new UpdateLogChannelListener(dbManager));
    
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
        ServerTextChannel channel = null;
        if(joinEvent.getServer().canYouCreateChannels()) {
          channel = joinEvent.getServer().createTextChannelBuilder().setName(discordApi.getYourself() + "-log").create().join();
        }
        dbManager.upsert(new GuildEntity(serverId, joinEvent.getServer().getName(), channel == null ? null : channel.getIdAsString()));
      }
      System.out.println("joined " + dbManager.findGuildById(serverId));  //log
    });
    
    // Suspicious words Listener
    var suspiciousWordsListener = new SuspiciousWordsListener(dbManager);
    discordApi.addMessageCreateListener(suspiciousWordsListener::onMessageCreate);
    
    // Spam listener
    var spamListener = new SpamListener(dbManager);
    discordApi.addMessageCreateListener(spamListener::onMessageCreate);
    
    var mentionListener = new MentionListener(dbManager);
    discordApi.addMessageCreateListener(mentionListener::onMessageCreate);
    
    // Commands listeners
    discordApi.addMessageCreateListener(event -> {
      if(event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
        var content = Arrays.asList(event.getMessageContent().split("\\s+"));
        var serverId = event.getServer().get().getIdAsString();
        var guild = dbManager.findGuildById(serverId);
        
        if(guild == null) {
          dbManager.upsert(new GuildEntity(serverId, event.getServer().get().getName(), null));
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
