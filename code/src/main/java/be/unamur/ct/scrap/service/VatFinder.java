package be.unamur.ct.scrap.service;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class VatFinder {

  private static final Logger logger = getLogger(VatFinder.class);

  private final Pattern vatPattern;

  public VatFinder() {
    // TODO add regex for NL and FR ??
    String VAT_REGEX = "(?i)((BE)?0([. -])?[0-9]{3}([. -])?[0-9]{3}([. -])?[0-9]{3})";
    this.vatPattern = Pattern.compile(VAT_REGEX);
  }


  /**
   * Normalizes a raw VAT number found on a web page to fit the format BExxxxxxxxxx where x is a digit
   *
   * @author Jules Dejaeghere
   * @param VAT Raw VAT number found on a web page
   * @return Nullable (if input is null) String with the normalized VAT number
   */
  public String normalizeVAT(String VAT) {
    if (VAT == null) {
      return null;
    }
    VAT = VAT.toUpperCase();
    VAT = VAT.replace(".", "")
        .replace("-", "")
        .replace(" ", "");
    VAT = (VAT.startsWith("BE") ? VAT : "BE" + VAT);
    return VAT;
  }

  /**
   * Determines if a normalized VAT number is valid.
   * A valid VAT number meet the following criteria:
   *  - Let BE0xxx xxx xyy a VAT number
   *  - The VAT number if valid if 97-(xxxxxxx mod 97) == yy
   *
   * @author Jules Dejaeghere
   * @param VAT   Normalized VAT number to check
   * @return      true if the VAT is valid, false otherwise
   */
  public boolean isValidVAT(String VAT) {
    VAT = VAT.substring(2);
    int head = Integer.parseInt(VAT.substring(0, VAT.length() - 2));
    int tail = Integer.parseInt(VAT.substring(8));
    return (97 - (head % 97)) == tail;
  }

  public List<String> findVatValues(String text) {
    List<String> vatList = new ArrayList<>();
    Matcher matcher = vatPattern.matcher(text);
    while (matcher.find()) {
      String group0 = matcher.group(0);
      logger.debug("group0 = {}", group0);
      String VAT = matcher.group(1);
      logger.debug("VAT = {}", VAT);
      VAT = normalizeVAT(VAT);
      vatList.add(VAT);
    }
    return vatList;
  }

  public List<String> findValidVatValues(String text) {
    List<String> vatList = findVatValues(text);
    return vatList.stream().filter(this::isValidVAT).collect(Collectors.toList());
  }

}
