package bot.data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "spam_messages")
public class SpamUrlEntity {
  private String id;
  private long sentDate;
  private String url;
  private Set<String> servers;
  
  public SpamUrlEntity() {
    if(servers == null) {
      servers = new HashSet<>();
    }
  }
  
  public SpamUrlEntity(long epocDate, String url, String serverId) {
    id = null;
    sentDate = epocDate;
    this.url = url;
    
    if(servers == null) {
      servers = new HashSet<>();
    }
    
    servers.add(serverId);
  }
  
  public String getId() {
    return id;
  }
  
  @Id
  public void setId(String id) {
    this.id = id;
  }
  
  public long getSentDate() {
    return sentDate;
  }
  
  public void setSentDate(long sentDate) {
    this.sentDate = sentDate;
  }
  
  public String getUrl() {
    return url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public Set<String> getServers() {
    return servers;
  }
  
  public void setServers(Set<String> servers) {
    this.servers = servers;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, sentDate, servers, url);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SpamUrlEntity other = (SpamUrlEntity) obj;
    return Objects.equals(id, other.id) && sentDate == other.sentDate && Objects.equals(servers, other.servers)
            && Objects.equals(url, other.url);
  }
  
  @Override
  public String toString() {
    return "SpamUrlEntity [id=" + id + ", sentDate=" + sentDate + ", url=" + url + ", servers=" + servers + "]";
  }
}
