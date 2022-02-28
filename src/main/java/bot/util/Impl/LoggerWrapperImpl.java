package bot.util.Impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import bot.util.LoggerWrapper;
import bot.util.Loglevel;

@Component
public class LoggerWrapperImpl implements LoggerWrapper {
  private Logger logger = LogManager.getLogger(LoggerWrapperImpl.class);
  
  @Override
  public void log(Loglevel lvl, String log) {
    switch (lvl) {
      case INFO: 
        logger.info(log);
        break;
      case WARN: 
        logger.warn(log);
        break;
      case ERROR:
        logger.error(log);
        break;
    }
  }
}
