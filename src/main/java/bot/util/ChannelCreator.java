package bot.util;

import org.javacord.api.entity.server.Server;

public interface ChannelCreator {
  public String create(Server server, final String channelName);
}
