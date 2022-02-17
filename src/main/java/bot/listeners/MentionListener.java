package bot.listeners;

import java.util.concurrent.TimeUnit;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;
import bot.util.Misc;

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
      if(!Misc.isAllowed(event, discordApi)) return;
  
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());

      if (event.getChannel().canYouWrite()) {
        var embed = Misc.getInfo(guild);
        var itr = embed.listIterator();

        event.getChannel().sendMessage(embed.get(0)).thenAccept(msg -> { 
          msg.addReactions("◀️", "▶️").exceptionally(ExceptionLogger.get());
          
          // reaction listener
          msg.addReactionAddListener(reactionEvent -> {
            if (reactionEvent.getEmoji().equalsEmoji("◀️")) {

              // if no user/myself - bail
              if (reactionEvent.getUser().isPresent() && reactionEvent.getUser().get().isYourself())
                return;

              if (!itr.hasPrevious()) {
                reactionEvent.removeReaction().exceptionally(ExceptionLogger.get());
                return;
              }

              reactionEvent.editMessage(itr.previous())
                      .thenCompose(message -> reactionEvent.removeReaction())
                      .exceptionally(ExceptionLogger.get());
              
            } else if (reactionEvent.getEmoji().equalsEmoji("▶️")) {

              // if no user/myself - bail
              if (reactionEvent.getUser().isPresent() && reactionEvent.getUser().get().isYourself())
                return;

              if (!itr.hasNext()) {
                reactionEvent.removeReaction().exceptionally(ExceptionLogger.get());
                return;
              }

              reactionEvent.editMessage(itr.next())
                      .thenCompose(message -> reactionEvent.removeReaction())
                      .exceptionally(ExceptionLogger.get());
            }
          }).removeAfter(2, TimeUnit.MINUTES);
        }).exceptionally(ExceptionLogger.get());
      }
    }
  }
}
