package bot.listeners;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;

import bot.dal.DBManager;

public class MentionListener implements MessageCreateListener {
  private Logger logger = LogManager.getLogger(MentionListener.class);
  private DBManager dbManager;

  public MentionListener(DBManager dbManager) {
    this.dbManager = dbManager;
  }

  @Override
  public void onMessageCreate(MessageCreateEvent event) {
    if(event.getMessageAuthor().asUser().isPresent() && event.getMessage().getMentionedUsers().contains(event.getApi().getYourself())) {
      var guild = dbManager.findGuildById(event.getServer().get().getIdAsString());
      if (guild == null) return;
      
      logger.info("invoking " + this.getClass().getName() + " for server " + guild.getId());
      
      if(event.getChannel().canYouWrite()) {
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
        
          if(event.getChannel().canYouManageMessages()) {
            event.getMessage().delete().exceptionally(ExceptionLogger.get());            
          }
          
          new MessageBuilder().setEmbed(embed)
          .addComponents(ActionRow.of(Button.danger("delete", "🗑️")))
          .send(event.getChannel())
          .thenAccept(msg -> msg
                  .addButtonClickListener(clickEvent -> clickEvent
                          .getButtonInteraction()
                          .getMessage()
                          .delete()
                          .exceptionally(ExceptionLogger.get()))
                  .removeAfter(1, TimeUnit.MINUTES)
                  .addRemoveHandler(() -> {
                    if(msg.canYouDelete()) {
                      msg.delete().exceptionally(ExceptionLogger.get());
                    }
                  }))
          .exceptionally(ExceptionLogger.get());
      }
    }
  }
}
