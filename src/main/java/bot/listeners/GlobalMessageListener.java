package bot.listeners;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.util.Misc;

@Service
public class GlobalMessageListener implements MessageCreateListener {
  private GuildDao guildDao; 
  private MentionListener mentionListener;
  private SuspiciousWordsListener suspiciousWordsListener;
  private UrlListener urlListener;
  private EveryoneListener everyoneListener;
  private SpamListener spamListener;
  
  @Autowired
  public void setGuildDao(GuildDao guildDao) {
    this.guildDao = guildDao;
  }
  
  @Autowired
  public void setMentionListener(MentionListener mentionListener) {
    this.mentionListener = mentionListener;
  }
  
  @Autowired
  public void setEveryoneListener(EveryoneListener everyoneListener) {
    this.everyoneListener = everyoneListener;
  }
  
  @Autowired
  public void setSuspiciousWordsListener(SuspiciousWordsListener suspiciousWordsListener) {
    this.suspiciousWordsListener = suspiciousWordsListener;
  }
  
  @Autowired
  public void setUrlListener(UrlListener urlListener) {
    this.urlListener = urlListener;
  }
  
  @Autowired
  public void setSpamListener(SpamListener spamListener) {
    this.spamListener = spamListener;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
      // fire for every user
      mentionListener.onMessageCreate(event, guild);
      
      // user is allowed (admin/server owner) - ignore their message
      if(event.getMessageAuthor().asUser().isPresent() && Misc.isUserAllowed(event)) {
        return;              
      }

      everyoneListener.onMessageCreate(event, guild);
      
      suspiciousWordsListener.onMessageCreate(event, guild);
      
      spamListener.onMessageCreate(event, guild);
      
      urlListener.onMessageCreate(event, guild);
    }); // guild.ifPresent
  }
}
