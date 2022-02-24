package bot.listeners;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;
import bot.dal.DBManager;
import bot.util.Misc;

/*
 * TODO
 * itr doesnt work properly
 */
public class InfoListener implements MessageCreateListener {
  private final Logger logger = LogManager.getLogger(InfoListener.class);
  private DBManager dbManager;
  
  public InfoListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }

  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent()) {
      if(!Misc.isUserAllowed(event, event.getApi())) return;
  
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if(!guild.getGuildName().equals(event.getServer().get().getName())) {
        guild.setGuildName(event.getServer().get().getName());
        dbManager.upsert(guild);
      }
      
      logger.info("invoking " + this.getClass().getName() + " for server " + guild.getId());
      
      if(event.getChannel().canYouManageMessages()) {
        event.getMessage().delete();
      }
      
      if (event.getChannel().canYouWrite()) {
        var embeds = Misc.getInfo(guild, event.getServer().get());
        
        // components (buttons) listener
        var itr = embeds.listIterator();
        
        new MessageBuilder().setEmbed(itr.next())
                .addComponents(ActionRow.of(Button.primary("previous", "◀️"), Button.primary("next", "▶️"), Button.primary("delete", "🗑️")))
                .send(event.getChannel())
                .thenAccept(msg -> msg.addMessageComponentCreateListener(componentEvent -> {
                  switch(componentEvent.getMessageComponentInteraction().getCustomId()) {
                    case "next":
                      if(itr.hasNext()) {
                        var old = componentEvent.getMessageComponentInteraction().getMessage().getComponents();
                        componentEvent.getMessageComponentInteraction()
                                  .createOriginalMessageUpdater()
                                  .removeAllComponents()
                                  .removeAllEmbeds()
                                  .addEmbed(itr.next())
                                  .addComponents(old.get(0))
                                  .update()
                                  .exceptionally(ExceptionLogger.get());             
                      } else {
                        componentEvent.getMessageComponentInteraction().acknowledge().exceptionally(ExceptionLogger.get());
                      }
                      break;
                    case "previous":
                      if(itr.hasPrevious()) {
                        var old = componentEvent.getMessageComponentInteraction().getMessage().getComponents();
                        componentEvent.getMessageComponentInteraction()
                                  .createOriginalMessageUpdater()
                                  .removeAllComponents()
                                  .removeAllEmbeds()
                                  .addEmbed(itr.previous())
                                  .addComponents(old.get(0))
                                  .update()
                                  .exceptionally(ExceptionLogger.get());             
                      } else {
                        componentEvent.getMessageComponentInteraction().acknowledge().exceptionally(ExceptionLogger.get());
                      }
                      break;
                    case "delete":  
                      if(event.getChannel().canYouManageMessages()) {
                        componentEvent.getMessageComponentInteraction()
                                      .getMessage()
                                      .delete()
                                      .exceptionally(ExceptionLogger.get());                        
                      }
                      break;         
                  }
                })
                .removeAfter(2, TimeUnit.MINUTES))
                .exceptionally(ExceptionLogger.get());
          
        

//        var reactionListener = new ServerInfoReactionListener(embeds);
//        event.getChannel().sendMessage(embeds.get(0)).thenAccept(msg -> { 
//          msg.addReactions("◀️", "▶️").exceptionally(ExceptionLogger.get());
//          
//          // reaction listener
//          msg.addReactionAddListener(reactionListener::onReactionAdd).removeAfter(2, TimeUnit.MINUTES);
//        })
//        .exceptionally(ExceptionLogger.get());
      }
    }
  }
}
