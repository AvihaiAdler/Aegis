package bot.util.Impl;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import bot.dal.SpamUrlsDao;
import bot.util.CacheOrganizer;

@Service
public class CacheOrganizerImpl implements CacheOrganizer {
  private Logger logger = LoggerFactory.getLogger(CacheOrganizerImpl.class);
  private SpamUrlsDao spamUrlsDao;
  private int intervalDays;
  private int spamThreshold;
  
  @Autowired
  public void setSpamUrlsDao(SpamUrlsDao spamUrlsDao) {
    this.spamUrlsDao = spamUrlsDao;
  }
  
  @Value("${interval.days:2}")
  public void setIntervalDays(int intervalDays) {
    this.intervalDays = intervalDays;
  }
  
  @Value("${spam.threshold:2}")
  public void setSpamThreshold(int spamThreshold) {
    this.spamThreshold = spamThreshold;
  }
  
  @Override
  public void organize() {
    logger.info("cleaning cache");
    
    var now = Instant.now().getEpochSecond();
    long from = now - (intervalDays + 1) * 24 * 60 * 60;
    long to = now - intervalDays * 24 * 60 * 60;        // days * hours * minutes * seconds
    
    // gather all spam entities saved from 3 days ago to 2 days ago
    spamUrlsDao.findAllBySentDateBetween(from, to).forEach(spamUrl -> {
      if(spamUrl.getServers().size() < spamThreshold) {
        spamUrlsDao.delete(spamUrl);
        logger.info("deleted " + spamUrl + " wasn't considered spam by " + spamThreshold + " number of servers");
      }
    });
  }
}
