package bot.util;

import java.util.concurrent.CompletableFuture;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;

import bot.data.GuildEntity;

public interface MessageSender {
  public CompletableFuture<Message> send(TextChannel channel, final String message, GuildEntity guild);
}
