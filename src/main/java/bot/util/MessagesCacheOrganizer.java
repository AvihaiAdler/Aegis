package bot.util;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import bot.dal.MessagesDao;

@Service
public class MessagesCacheOrganizer implements CacheOrganizer {
  private MessagesDao messagesDao;
  private int intervalSeconds;
  
  @Autowired
  public void setMessagesDao(MessagesDao messagesDao) {
    this.messagesDao = messagesDao;
  }
  
  @Value("${interval.seconds:30}")
  public void setIntervalSeconds(int intervalSeconds) {
    this.intervalSeconds = intervalSeconds;
  }
  
  @Override
  public void organize() {
    var now = Instant.now().toEpochMilli();
    var from = now - (intervalSeconds*1000)*2;
    var to = now - intervalSeconds*1000;
    messagesDao.findAllBySentTimeMillisBetween(from, to).forEach(msg -> {
      messagesDao.delete(msg);
    });
  }
}
