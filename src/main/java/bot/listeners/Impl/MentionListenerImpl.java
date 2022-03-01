package bot.listeners.Impl;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import bot.dal.GuildDao;
import bot.listeners.MentionListener;
import bot.util.Misc;

@Service
public class MentionListenerImpl implements MentionListener {
  private Logger logger = LogManager.getLogger();
  private GuildDao guildDao;
  
  @Autowired
  public void setGuildDao(GuildDao guildDao) {
    this.guildDao = guildDao;
  }

  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    event.getMessageAuthor().asUser().ifPresent(usr -> {
      
      // if the message contains more than just @Aegis - bail
      if(event.getMessage().getMentionedUsers().size() > 1) return;
      if(!event.getMessage().getMentionedUsers().contains(event.getApi().getYourself())) return;
      if(event.getMessageContent().split("\\s+").length > 1) return;
      
      guildDao.findById(event.getServer().get().getIdAsString()).ifPresent(guild -> {
        var embed = new EmbedBuilder()
                .setTitle("Commands")
                .addField("@" + event.getApi().getYourself().getName(), "dispaly the commads table")
                .addField(guild.getPrefix() + "info", "display all the info " +  event.getApi().getYourself().getName().toString() + " holds on the server")
                .addField(guild.getPrefix() + "logto <channel id>", event.getApi().getYourself().getName() + " will direct it's logs to the channel specified")
                .addField(guild.getPrefix() + "prefix <character/s>", event.getApi().getYourself().getName() + " will replace the current prefix with the one specified")
                .addField(guild.getPrefix() + "threshold <integer>", event.getApi().getYourself().getName() + " will set the threshold for the one specifier")
                .addField(guild.getPrefix() + "restrict", event.getApi().getYourself().getName() + " will operate in restrict mode (see documentation)")
                .addField(guild.getPrefix() + "unrestrict", event.getApi().getYourself().getName() + " will operate in unrestrict mode (see documentation)")
                .addField(guild.getPrefix() + "suspect <a list of words seperated by spaces>", event.getApi().getYourself().getName() + " will add the words to the suspicious list")
                .addField(guild.getPrefix() + "unsuspect <a list of words seperated by spaces>", event.getApi().getYourself().getName() + " will remove the words from the suspicious list")
                .addField(guild.getPrefix() + "block <a list of urls seperated by spaces>", event.getApi().getYourself().getName() + " will add the urls to the blocked list")
                .addField(guild.getPrefix() + "unblock <a list of urls seperated by spaces>", event.getApi().getYourself().getName() + " will removed the urls from the blocked list");
        
        new MessageBuilder().setEmbed(embed)
        .addComponents(ActionRow.of(Button.danger("delete", "🗑️")))
        .send(event.getChannel())
        .thenAccept(msg -> msg
                .addButtonClickListener(clickEvent -> clickEvent
                        .getButtonInteraction()
                        .getMessage()
                        .delete()
                        .exceptionally(e -> {
                          logger.error("failed to delete a command embed in " + guild.getLogChannelId() + " server: "
                                  + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
                          return null;
                        }))
                .removeAfter(1, TimeUnit.MINUTES)
                .addRemoveHandler(() -> msg.delete().exceptionally(e -> {
                  logger.error("failed to delete a command embed with a hadler in " + guild.getLogChannelId() + " server: "
                          + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + " user deleted manually\n" + Misc.parseThrowable(e));
                  return null;
                })))
        .exceptionally(e -> {
          logger.error("failed to attach a listener in " + guild.getLogChannelId() + " server: "
                  + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
          return null;
        })
        .thenRun(() -> event.getMessage().delete()) // delete the user command
        .exceptionally(e -> {
          logger.error("failed to delete a command in " + guild.getLogChannelId() + " server: "
                  + guild.getGuildName() + " (" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
          return null;
        });    
      }); // guild.ifPresent
      
    }); //event.getMessageAuthor().asUser().ifPresent
  }
}
