package bot.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GuildBoundary {
  private String guildId;
  private String prefix;
  private Integer threshold;
  private Map<String, Integer> suspiciousWords;
  private List<String> blockedUrls;
  
  public GuildBoundary(String guildId) {
    this.guildId = guildId;
    prefix = "!";
    threshold = 0;
    suspiciousWords = new HashMap<>();
    blockedUrls = new ArrayList<>();
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public Integer getThreshold() {
    return threshold;
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  public Map<String, Integer> getSuspicousWords() {
    return suspiciousWords;
  }

  public void setSuspicousWords(Map<String, Integer> suspicousWords) {
    this.suspiciousWords = suspicousWords;
  }

  public List<String> getBlockedUrls() {
    return blockedUrls;
  }

  public void setBlockedUrls(List<String> blockedUrls) {
    this.blockedUrls = blockedUrls;
  }

  @Override
  public int hashCode() {
    return Objects.hash(guildId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    GuildBoundary other = (GuildBoundary) obj;
    return Objects.equals(guildId, other.guildId);
  }

  @Override
  public String toString() {
    return "Guild [guildId=" + guildId + ", prefix=" + prefix + ", threshold=" + threshold + ", suspicousWords="
            + suspiciousWords + ", blockedUrls=" + blockedUrls + "]";
  }
}
