package bot.listeners;

import org.javacord.api.event.message.MessageCreateEvent;

import bot.data.GuildEntity;

public interface GeneralListener {
  public void onMessageCreate(MessageCreateEvent event, GuildEntity guild);
}
