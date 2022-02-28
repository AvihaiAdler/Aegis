package bot.listeners.Impl;

import java.util.concurrent.TimeUnit;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bot.dal.GuildDao;
import bot.listeners.InfoListener;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.Misc;

/*
 * TODO
 * itr doesnt work properly
 */
@Service
public class InfoListenerImpl implements InfoListener {
  private LoggerWrapper loggerWrapper;
  private GuildDao guildDao;

  @Autowired
  public void setLoggerWrapper(LoggerWrapper loggerWrapper) {
    this.loggerWrapper = loggerWrapper;
  }
  
  @Autowired
  public void setGuildDao(GuildDao guildDao) {
    this.guildDao = guildDao;
  }
  
  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      // user doesn't have permission to invoke the command
      if (!Misc.isUserAllowed(event, event.getApi())) return; 
      
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        // update guild name (in case it was changed between calls to info)
        var updated = guild;
        if (!guild.getGuildName().equals(event.getServer().get().getName())) {
          guild.setGuildName(event.getServer().get().getName());
          updated = guildDao.save(guild);

          loggerWrapper.log(Loglevel.INFO, "changed the name of " + event.getServer().get().getIdAsString() + " to "+ event.getServer().get().getName());
        }  
        
        var embeds = Misc.getInfo(updated, event.getServer().get());        
        // components (buttons) listener
        var itr = embeds.listIterator();
        
        new MessageBuilder().setEmbed(itr.next())
        .addComponents(ActionRow.of(Button.secondary("previous", "◀️"), Button.secondary("next", "▶️"), Button.danger("delete", "🗑️")))
        .send(event.getChannel())
        .thenAccept(msg -> msg.addButtonClickListener(clickEvent -> {
          switch (clickEvent.getButtonInteraction().getCustomId()) {
            case "next":
              if (itr.hasNext()) {
                var old = clickEvent.getButtonInteraction().getMessage().getComponents();
                clickEvent.getButtonInteraction()
                .createOriginalMessageUpdater()
                .removeAllComponents()
                .removeAllEmbeds()
                .addEmbed(itr.next())
                .addComponents(old.get(0))
                .update()
                .exceptionally(e -> {
                  loggerWrapper.log(Loglevel.ERROR, "failed to edit embed in " + guild.getLogChannelId() + " server: "
                          + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + e.getMessage());
                  return null;
                });
              } else {
                clickEvent.getButtonInteraction().acknowledge().exceptionally(ExceptionLogger.get());
              }
              break;
            case "previous":
              if (itr.hasPrevious()) {
                var old = clickEvent.getButtonInteraction().getMessage().getComponents();
                clickEvent.getButtonInteraction()
                .createOriginalMessageUpdater()
                .removeAllComponents()
                .removeAllEmbeds()
                .addEmbed(itr.previous())
                .addComponents(old.get(0))
                .update()
                .exceptionally(e -> {
                  loggerWrapper.log(Loglevel.ERROR, "failed to edit embed in " + guild.getLogChannelId() + " server: "
                          + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + e.getMessage());
                  return null;
                });
              } else {
                clickEvent.getButtonInteraction().acknowledge().exceptionally(ExceptionLogger.get());
              }
              break;
            case "delete":
              if (event.getChannel().canYouManageMessages()) {
                clickEvent.getButtonInteraction()
                .getMessage()
                .delete()
                .exceptionally(e -> {
                  loggerWrapper.log(Loglevel.ERROR, "failed to delete an info embed in " + guild.getLogChannelId() + " server: "
                          + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + e.getMessage());
                  return null;
                });
              }
              break;
          }
        })
                .removeAfter(1, TimeUnit.MINUTES)
                .addRemoveHandler(() -> {
                  msg.delete().exceptionally(e -> {
                    loggerWrapper.log(Loglevel.ERROR, "failed to delete an info embed with a handler in " + guild.getLogChannelId() + " server: "
                            + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + e.getMessage() + " embed deleted manually");
                    return null;
                  });
                }))
        .exceptionally(e -> {
          loggerWrapper.log(Loglevel.ERROR, "failed to attach a listener in " + guild.getLogChannelId() + " server: "
                  + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + e.getMessage());
          return null;
        })
        .thenRun(() -> event.getMessage().delete()) // remove the command to prevent clutter
        .exceptionally(e -> {
          loggerWrapper.log(Loglevel.ERROR, "failed to delete a command in " + guild.getLogChannelId() + " server: "
                  + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + e.getMessage());
          return null;
        });
      }); //guild.ifPresent
    }); //event.getMessageAuthor().asUser().ifPresent
  }
}
