package bot.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ConfigManager {
  private static ConfigManager instance;
  private Map<String, String> properties;

  private ConfigManager() {
    properties = new HashMap<>();
  }
  
  public synchronized static ConfigManager getInstance() {
    if(instance == null) {
      instance = new ConfigManager();
    }
    return instance;
  }
  
  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
  
  public void populate() throws IOException, NullPointerException {
    var inputStream = getClass().getClassLoader().getResourceAsStream("config.properties");
    var configValues = new Properties();
    configValues.load(inputStream);
      
    properties.put("token", configValues.getProperty("token"));
    properties.put("connectionString", configValues.getProperty("connection.string"));
    properties.put("db", configValues.getProperty("db"));
    properties.put("collection", configValues.getProperty("collection"));
    inputStream.close();
  }

  @Override
  public int hashCode() {
    return Objects.hash(properties);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ConfigManager other = (ConfigManager) obj;
    return Objects.equals(properties, other.properties);
  }
  
  
}
