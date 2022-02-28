package bot.util.Impl;

import java.util.concurrent.CompletableFuture;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bot.data.GuildEntity;
import bot.util.LoggerWrapper;
import bot.util.Loglevel;
import bot.util.MessageSender;

@Service
public class MessageSenderImpl implements MessageSender {
  private LoggerWrapper loggerWrapper;
  
  @Autowired
  public void setLoggerWrapper(LoggerWrapper loggerWrapper) {
    this.loggerWrapper = loggerWrapper;
  }
  
  @Override
  public CompletableFuture<Message> send(TextChannel channel, final String message, GuildEntity guild) {
     return new MessageBuilder()
            .setContent(message)
            .send(channel)
            .exceptionally(e -> {
              loggerWrapper.log(Loglevel.ERROR, "failed to send a message in channel: " + guild.getLogChannelId() + " server: "
                      + guild.getGuildName() + "(" + guild.getId() + ")" + "\nreason: " + e.getCause().getMessage());
              return null;
            });
  }
}
