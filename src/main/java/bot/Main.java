package bot;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

public class Main {
  private final static Logger logger = LogManager.getLogger(Main.class);
  
  public static void main(final String[] args) throws Exception {
    final Map<String, MessageCreateListener> commands = Collections.synchronizedMap(new HashMap<>());    
    var properties = ConfigManager.getInstance();

    try {
      properties.populate();
    } catch (IOException e) {
      logger.error(e.getMessage());
      System.exit(1);
    } catch (NullPointerException ne) {
      logger.error(ne.getMessage());
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
    logger.info("successfuly logged in");
    
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
    logger.info("added command listeners");
    
    // Server leave listener
    discordApi.addServerLeaveListener(leaveEvent -> {
      var serverId = leaveEvent.getServer().getIdAsString();
      if (dbManager.findGuildById(serverId) != null) {
        dbManager.delete(serverId);
      }
      logger.info("Left " + serverId);
    });

    // Server joined listener
    discordApi.addServerJoinListener(joinEvent -> {
      var serverId = joinEvent.getServer().getIdAsString();
      System.out.println(dbManager.findGuildById(serverId));
      if (dbManager.findGuildById(serverId) == null) {
        ServerTextChannel channel = null;
        var loggingChannelName = discordApi.getYourself().getName() + "-log";
        
        if(!joinEvent.getServer().getChannelsByName(loggingChannelName).isEmpty()) {
          channel = joinEvent.getServer()
                  .getChannelsByName(loggingChannelName).get(0)
                  .asServerTextChannel().get();
        } else if(joinEvent.getServer().canYouCreateChannels()) {
          channel = joinEvent.getServer()
                  .createTextChannelBuilder()
                  .setName(loggingChannelName)
                  .create()
                  .join();
        }
        
        dbManager.upsert(new GuildEntity(serverId, joinEvent.getServer().getName(), channel == null ? null : channel.getIdAsString()));
        logger.info("joined " + dbManager.findGuildById(serverId).getGuildName());
      }
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
