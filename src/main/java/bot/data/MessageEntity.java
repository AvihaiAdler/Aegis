package bot.data;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
public class MessageEntity {
  private String id;
  private String channelId;
  private long sentTimeMillis;
  private String username;  // username is unique
  
  public MessageEntity() {

  }
  
  public MessageEntity(String channelId, long sentTimeMillis, String username) {
    id = null;
    this.channelId = channelId;
    this.sentTimeMillis = sentTimeMillis;
    this.username = username;
  }

  public String getId() {
    return id;
  }

  @Id
  public void setId(String id) {
    this.id = id;
  }

  public long getSentTimeMillis() {
    return sentTimeMillis;
  }

  public void setSentTimeMillis(long sentTimeMillis) {
    this.sentTimeMillis = sentTimeMillis;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getDiscriminatedUserName() {
    return username;
  }

  public void setDiscriminatedUserName(String discriminatedUserName) {
    this.username = discriminatedUserName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(channelId, username, id, sentTimeMillis);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MessageEntity other = (MessageEntity) obj;
    return Objects.equals(channelId, other.channelId)
            && Objects.equals(username, other.username) && Objects.equals(id, other.id)
            && Objects.equals(sentTimeMillis, other.sentTimeMillis);
  }

  @Override
  public String toString() {
    return "MessageEntity [id=" + id + ", channelId=" + channelId + ", sentTimeMillis=" + sentTimeMillis
            + ", discriminatedUserName=" + username + "]";
  }
}
