package bot.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import bot.dal.GuildDao;
import bot.listeners.BlockListener;
import bot.listeners.InfoListener;
import bot.listeners.MentionListener;
import bot.listeners.PrefixListener;
import bot.listeners.RestrictListener;
import bot.listeners.SpamListener;
import bot.listeners.SuspectListener;
import bot.listeners.SuspiciousWordsListener;
import bot.listeners.ThresholdListener;
import bot.listeners.UnblockListener;
import bot.listeners.UnrestrictListener;
import bot.listeners.UnsuspectListener;
import bot.listeners.UpdateLogChannelListener;
import bot.listeners.UrlListener;

@Configuration
public class Config {
  private Logger logger = LogManager.getLogger();
  private GuildDao guildDao;
  private RegisterServer registerServer;
  
  // listeners
  private BlockListener blockListener;
  private InfoListener infoListener;
  private MentionListener mentionListener;
  private PrefixListener prefixListener;
  private RestrictListener restrictListener;
  private SpamListener spamListener;
  private SuspectListener suspectListener;
  private SuspiciousWordsListener suspiciousWordsListener;
  private ThresholdListener thresholdListener;
  private UnblockListener unblockListener;
  private UnrestrictListener unrestrictListener;
  private UnsuspectListener unsuspectListener;
  private UpdateLogChannelListener updateLogChannelListener;
  private UrlListener urlListener;
  
  @Autowired
  public void setGuildDao(GuildDao guildDao) {
    this.guildDao = guildDao;
  }
  
  @Autowired
  public void setRegisterServer(RegisterServer registerServer) {
    this.registerServer = registerServer;
  }
  
  @Autowired
  public void setBlockListener(BlockListener blockListener) {
    this.blockListener = blockListener;
  }
  
  @Autowired
  public void setInfoListener(InfoListener infoListener) {
    this.infoListener = infoListener;
  }
  
  @Autowired
  public void setMentionListener(MentionListener mentionListener) {
    this.mentionListener = mentionListener;
  }
  
  @Autowired
  public void setPrefixListener(PrefixListener prefixListener) {
    this.prefixListener = prefixListener;
  }
  
  @Autowired
  public void setRestrictListener(RestrictListener restrictListener) {
    this.restrictListener = restrictListener;
  }
  
  @Autowired
  public void setSpamListener(SpamListener spamListener) {
    this.spamListener = spamListener;
  }
  
  @Autowired
  public void setSuspectListener(SuspectListener suspectListener) {
    this.suspectListener = suspectListener;
  }
  
  @Autowired
  public void setSuspiciousWordsListener(SuspiciousWordsListener suspiciousWordsListener) {
    this.suspiciousWordsListener = suspiciousWordsListener;
  }
  
  @Autowired
  public void setThresholdListener(ThresholdListener thresholdListener) {
    this.thresholdListener = thresholdListener;
  }
  
  @Autowired
  public void setUnblockListener(UnblockListener unblockListener) {
    this.unblockListener = unblockListener;
  }
  
  @Autowired
  public void setUnrestrictListener(UnrestrictListener unrestrictListener) {
    this.unrestrictListener = unrestrictListener;
  }
  
  @Autowired
  public void setUnsuspectListener(UnsuspectListener unsuspectListener) {
    this.unsuspectListener = unsuspectListener;
  }
  
  @Autowired
  public void setUpdateLogChannelListener(UpdateLogChannelListener updateLogChannelListener) {
    this.updateLogChannelListener = updateLogChannelListener;
  }
  
  @Autowired
  public void setUrlListener(UrlListener urlListener) {
    this.urlListener = urlListener;
  }
  
  @Bean
  public DiscordApi discordApi() {
    final Map<String, MessageCreateListener> commands = Collections.synchronizedMap(new HashMap<>());

    var discordApi = new DiscordApiBuilder()
        .setToken(System.getenv("TOKEN"))
        .setAllNonPrivilegedIntentsExcept(Intent.DIRECT_MESSAGES,
            Intent.DIRECT_MESSAGE_TYPING,
            Intent.DIRECT_MESSAGE_REACTIONS,
            Intent.GUILD_INVITES,
            Intent.GUILD_WEBHOOKS,
            Intent.GUILD_MESSAGE_TYPING,
            Intent.GUILD_WEBHOOKS)
        .login()
        .join();
    logger.info(discordApi.getYourself().getDiscriminatedName() + " successfuly logged in");
  
    discordApi.setMessageCacheSize(30, 60 * 10); // store only 30 messages per channel for 10 minutes
    discordApi.updateActivity(ActivityType.WATCHING, "messages");

    // Commands listeners
    commands.put("restrict", restrictListener);
    commands.put("unrestrict", unrestrictListener);
    commands.put("suspect", suspectListener);
    commands.put("unsuspect", unsuspectListener);
    commands.put("prefix", prefixListener);
    commands.put("block", blockListener);
    commands.put("unblock", unblockListener);
    commands.put("threshold", thresholdListener);
    commands.put("logto", updateLogChannelListener);
    commands.put("info", infoListener);
    logger.info("added command listeners");

    // Server leave listener
    discordApi.addServerLeaveListener(leaveEvent -> {
      final var serverId = leaveEvent.getServer().getIdAsString();
      guildDao.findById(serverId).ifPresent(guild -> guildDao.delete(guild));
      
      logger.info("Left " + serverId);
    });
  
    // Server joined listener
    discordApi.addServerJoinListener(joinEvent -> {
      registerServer.registerServer(joinEvent);
    });
  
    discordApi.addMessageCreateListener(suspiciousWordsListener);
    discordApi.addMessageCreateListener(spamListener);
    discordApi.addMessageCreateListener(urlListener);
    discordApi.addMessageCreateListener(mentionListener);
    
    // Commands listeners
    discordApi.addMessageCreateListener(event -> {
      if (event.isServerMessage() && !event.getMessageAuthor().isBotUser()) {
        var content = Arrays.asList(event.getMessageContent().split("\\s+"));
        var serverId = event.getServer().get().getIdAsString();
        
        guildDao.findById(serverId).ifPresentOrElse(guild -> {
          if(event.getMessageContent().startsWith(guild.getPrefix())) {
            var listener = commands.get(content.get(0).split(Pattern.quote(guild.getPrefix()))[1]);
            
            if (listener != null) {
              listener.onMessageCreate(event);
            }
          }
        }, () ->/* guild doesn't exists in the DB*/ registerServer.registerServer(event)); // guild.ifPresent  
      }
    }); // discordApi.addMessageCreateListener
    return discordApi;
  }
}
