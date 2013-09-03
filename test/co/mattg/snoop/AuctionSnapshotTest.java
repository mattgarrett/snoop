package co.mattg.snoop;

import static org.junit.Assert.*;

import org.junit.Test;

public class AuctionSnapshotTest {

  @Test
  public void testCompareToDifferentQuantity() {
    AuctionSnapshot first = new AuctionSnapshot("shoes", 3, 50, 20, null, 2000);
    AuctionSnapshot second = new AuctionSnapshot("shoes", 3, 50, 10, null, 2000);
    
    assertEquals(0, first.compareTo(second));
    assertEquals(0, second.compareTo(first));
  }
  
  @Test
  public void testCompareToDifferentTimestamp() {
    AuctionSnapshot first = new AuctionSnapshot("shoes", 3, 50, 20, null, 1900);
    AuctionSnapshot second = new AuctionSnapshot("shoes", 3, 50, 20, null, 2000);
    
    assertEquals(0, first.compareTo(second));
    assertEquals(0, second.compareTo(first));
  }
  
  @Test
  public void testCompareToDifferentProduct() {
    AuctionSnapshot first = new AuctionSnapshot("shoes", 3, 50, 20, null, 1900);
    AuctionSnapshot second = new AuctionSnapshot("coat", 3, 50, 20, null, 1900);
    
    assertFalse(0 == first.compareTo(second));
    assertFalse(0 == second.compareTo(first));
  }
  
  @Test
  public void testCompareToDifferentPrices() {
    AuctionSnapshot first = new AuctionSnapshot("shoes", 6, 50, 20, null, 2000);
    AuctionSnapshot second = new AuctionSnapshot("shoes", 3, 50, 20, null, 2000);
  
    assertFalse(0 == first.compareTo(second));
    assertFalse(0 == second.compareTo(first));
  }
}
