package bot.util;

public enum Loglevel {
  INFO("info"),
  WARN("warn"),
  ERROR("error");
  
  private final String level;
  
  private Loglevel(String level) {
    this.level = level;
  }
  
  public String toString() {
    return level;
  }
}
