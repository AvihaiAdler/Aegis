package bot.listeners;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

/*
 * TODO
 * itr doesnt work properly
 */
public class MentionListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(MentionListener.class);
  private DBManager dbManager;
  
  public MentionListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }

  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent() && event.getMessage().getMentionedUsers().contains(event.getApi().getYourself())) {
      if(!Misc.isUserAllowed(event, event.getApi())) return;
  
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      logger.info("invoking " + this.getClass().getName() + " for server " + guild.getId());
      
      if (event.getChannel().canYouWrite()) {
        var embed = Misc.getInfo(guild, event.getServer().get());
        var reactionListener = new ServerInfoReactionListener(embed.listIterator());
//        var componentListener = new ComponentListener(embed);

//        new MessageBuilder().setEmbed(embed.get(0))
//                .addComponents(ActionRow.of(Button.primary("previous", "◀️"), Button.primary("next", "▶️")))
//                .send(event.getChannel())
//                .thenAccept(msg -> msg.addMessageComponentCreateListener(componentListener).removeAfter(2, TimeUnit.MINUTES))
//                .exceptionally(ExceptionLogger.get());

        event.getChannel().sendMessage(embed.get(0)).thenAccept(msg -> { 
          msg.addReactions("◀️", "▶️").exceptionally(ExceptionLogger.get());
          
          // reaction listener
          msg.addReactionAddListener(reactionListener).removeAfter(2, TimeUnit.MINUTES);
        }).exceptionally(ExceptionLogger.get());
      }
    }
  }
}
