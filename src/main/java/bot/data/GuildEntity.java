package bot.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bson.codecs.pojo.annotations.BsonId;


public class GuildEntity {
  private String id;
  private String guildId;
  private String prefix;
  private boolean restricted;
  private int threshold;
  private Map<String, Integer> suspiciousWords;
  private Set<String> blockedUrls;
  
  public GuildEntity() {
    
  }
  
  public GuildEntity(String id) {
    this.id = guildId = id; 
    prefix = "!";
    threshold = 0;
    restricted = false;
    suspiciousWords = new HashMap<>();
    blockedUrls = new HashSet<>();
  }
  
  public String getId() {
    return id;
  }

  @BsonId
  public void setId(String id) {
    this.id = id;
  }

  public Map<String, Integer> getSuspiciousWords() {
    return suspiciousWords;
  }

  public void setSuspiciousWords(Map<String, Integer> suspiciousWords) {
    this.suspiciousWords = suspiciousWords;
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

  public int getThreshold() {
    return threshold;
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  public boolean getRestricted() {
    return restricted;
  }

  public void setRestricted(boolean restricted) {
    this.restricted = restricted;
  }

  public Set<String> getBlockedUrls() {
    return blockedUrls;
  }

  public void setBlockedUrls(Set<String> blockedUrls) {
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
    GuildEntity other = (GuildEntity) obj;
    return Objects.equals(guildId, other.guildId);
  }

  @Override
  public String toString() {
    return "GuildEntity [id=" + id + ", guildId=" + guildId + ", prefix=" + prefix + ", restricted=" + restricted
            + ", threshold=" + threshold + ", suspiciousWords=" + suspiciousWords + ", blockedUrls=" + blockedUrls
            + "]";
  }
}
