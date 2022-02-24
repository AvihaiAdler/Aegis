package bot.listeners;

import java.util.List;
import java.util.ListIterator;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

public class ComponentListener implements MessageComponentCreateListener {
  private List<EmbedBuilder> embeds;
  private ListIterator<EmbedBuilder> itr;
  
  public ComponentListener(List<EmbedBuilder> embeds) {
    this.embeds = embeds;
    itr = this.embeds.listIterator();
  }
  
  @Override
  public void onComponentCreate(MessageComponentCreateEvent event) {
    var interactionId = event.getMessageComponentInteraction().getCustomId();

    switch(interactionId) {
      case "previous":
        if(itr.hasPrevious()) {
          var old = event.getMessageComponentInteraction().getMessage().getComponents();
          event.getMessageComponentInteraction()
                .createOriginalMessageUpdater()
                .removeAllComponents()
                .removeAllEmbeds()
                .addEmbed(itr.previous())
                .addComponents(old.get(0))
                .update()
                .exceptionally(ExceptionLogger.get());             
        } else {
          event.getMessageComponentInteraction().acknowledge();
        }
      case "next":
        if(itr.hasNext()) {
          var old = event.getMessageComponentInteraction().getMessage().getComponents();
          event.getMessageComponentInteraction()
                .createOriginalMessageUpdater()
                .removeAllComponents()
                .removeAllEmbeds()
                .addEmbed(itr.next())
                .addComponents(old.get(0))
                .update()
                .exceptionally(ExceptionLogger.get());             
        } else {
          event.getMessageComponentInteraction().acknowledge();
        }
        break;
      default:
        System.out.println("not supported");
        break;
    }
  }
}
