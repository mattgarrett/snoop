package co.mattg.snoop;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * A utility class for getting the state of Auctions
 * from steepandcheap.com
 * 
 * @author matt
 */
public class AuctionWatcher {
  public static final String URL = "http://www.steepandcheap.com/";
  public static final String PRODUCT = "product_title";
  public static final String PRICE = "price";
  public static final String REDUCTION = "percent_off";
  public static final String QUANTITY = "total_remaining";
  public static final String IMAGE = "item_image";
  
  /**
   * @return an AuctionSnapshot representing the state
   * of the current auction on steepandcheap.com or null
   * if the current auction cannot be fetched
   */
  public AuctionSnapshot getAuctionSnapshot() {
    try {
      Document doc = Jsoup.connect(URL).get();
      AuctionSnapshot current = new AuctionSnapshot(doc.getElementById(PRODUCT).text(),
                          doc.getElementById(PRICE).text(),
                          doc.getElementById(REDUCTION).text(),
                          doc.getElementById(QUANTITY).text(),
                          doc.getElementById(IMAGE).html());
      return current;
    } catch (IOException e) {
      return null;
    }
  }
}
