package bot.listeners;

import java.util.List;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

public class ComponentListener implements MessageComponentCreateListener {
  private List<EmbedBuilder> embeds;
  private int index;
  
  public ComponentListener(List<EmbedBuilder> embeds) {
    this.embeds = embeds;
    index = 0;
  }
  
  @Override
  public void onComponentCreate(MessageComponentCreateEvent event) {
    var interactionId = event.getMessageComponentInteraction().getCustomId();

    switch(interactionId) {
      case "previous":
        if(index > 0) {
          var old = event.getMessageComponentInteraction().getMessage();
          event.getMessageComponentInteraction().getMessage()
              .createUpdater()
              .removeAllEmbeds()
              .addEmbed(embeds.get(index--))
              .addComponents(old.getComponents().get(0))
              .replaceMessage()
              .exceptionally(ExceptionLogger.get());
        }
      case "next":
        if(index < embeds.size() - 1) {
          var old = event.getMessageComponentInteraction().getMessage();
          event.getMessageComponentInteraction().getMessage()
              .createUpdater()
              .removeAllEmbeds()
              .addEmbed(embeds.get(index++))
              .addComponents(old.getComponents().get(0))
              .replaceMessage()
              .exceptionally(ExceptionLogger.get());
        }
        break;
      default:
        System.out.println("not supported");
        break;
    }
  }
}
