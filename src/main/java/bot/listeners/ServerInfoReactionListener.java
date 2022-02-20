package bot.listeners;

import java.util.ListIterator;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.util.logging.ExceptionLogger;

public class ServerInfoReactionListener implements ReactionAddListener {
  private ListIterator<EmbedBuilder> itr;
  
  public ServerInfoReactionListener(ListIterator<EmbedBuilder> itr) {
    this.itr = itr;
  }
  
  @Override
  public void onReactionAdd(ReactionAddEvent reactionEvent) {
    if (reactionEvent.getEmoji().equalsEmoji("◀️")) {

      // if myself - bail
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

      // if myself - bail
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
  }
}
