package bot.util.Impl;

import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.springframework.stereotype.Service;
import bot.data.GuildEntity;
import bot.util.MessageSender;
import bot.util.Misc;

@Service
public class MessageSenderImpl implements MessageSender {
  private Logger logger = LogManager.getLogger();
  
  @Override
  public CompletableFuture<Message> send(TextChannel channel, final String message, GuildEntity guild) {
     return new MessageBuilder()
            .setContent(message)
            .send(channel)
            .exceptionally(e -> {
              logger.error("failed to send a message in channel: " + guild.getLogChannelId() + " server: "
                      + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + Misc.parseThrowable(e));
              return null;
            });
  }
}
