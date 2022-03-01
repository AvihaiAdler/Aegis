package bot.listeners;

import org.javacord.api.event.message.MessageCreateEvent;

import bot.data.GuildEntity;

public interface SuspiciousWordsListener {
  public void onMessageCreate(MessageCreateEvent event, GuildEntity guild);
}
