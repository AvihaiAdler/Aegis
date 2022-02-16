package bot.data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.bson.codecs.pojo.annotations.BsonId;


public class GuildEntity {
  private String id;
  private String guildName;
  private String prefix;
  private boolean restricted;
  private int threshold;
  private Set<String> suspiciousWords;
  private Set<String> blockedUrls;
  
  public GuildEntity() {
    
  }
  
  public GuildEntity(String id, String guildName) {
    this.id = id;
    this.guildName = guildName; 
    prefix = "!";
    threshold = 0;
    restricted = false;
    suspiciousWords = new HashSet<>();
    blockedUrls = new HashSet<>();
  }
  
  public String getId() {
    return id;
  }

  @BsonId
  public void setId(String id) {
    this.id = id;
  }

  public Set<String> getSuspiciousWords() {
    return suspiciousWords;
  }

  public void setSuspiciousWords(Set<String> suspiciousWords) {
    this.suspiciousWords = suspiciousWords;
  }

  public String getGuildName() {
    return guildName;
  }

  public void setGuildName(String guildName) {
    this.guildName = guildName;
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
    return Objects.hash(id);
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
    return Objects.equals(id, other.id);
  }

  @Override
  public String toString() {
    return "GuildEntity [id=" + id + ", guild name=" + guildName + ", prefix=" + prefix + ", restricted=" + restricted
            + ", threshold=" + threshold + ", suspiciousWords=" + suspiciousWords + ", blockedUrls=" + blockedUrls
            + "]";
  }
}
