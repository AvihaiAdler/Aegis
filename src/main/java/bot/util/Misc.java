package bot.util;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Misc {
  public static boolean containsUrl(String potentialUrl) {
    final String urlRegex = "<?\\b(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>?";
    if(potentialUrl == null)
      return false;
    var content = Arrays.asList(potentialUrl.split("\\s+"))
            .stream()
            .filter(word -> Pattern.matches(urlRegex, word))
            .collect(Collectors.toList()); 
    return !content.isEmpty();
  }
}
