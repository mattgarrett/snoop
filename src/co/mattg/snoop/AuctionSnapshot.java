package co.mattg.snoop;

public class AuctionSnapshot implements Comparable<AuctionSnapshot>{
	public final String product;
	public final int price;
	public final int reduction;
	public final int quantity;
	public final long timestamp;
	public final String image;
	
	/*
	 * Returns a AuctionState representing the given product, price, discount, and quantity
	 * the image is an HTML chunk representing the product's image on the web
	 */
	//TODO(matt): refactor this mess out to AuctionWatcher
	public AuctionSnapshot(String product, String price, String reduction, String quantity, String image) {
		this(product.substring(0, product.length() - 1),
		    Integer.parseInt(price.substring(1, price.length() - 3)),
		    Integer.parseInt(reduction.substring(0, reduction.length() - 5)),
		    Integer.parseInt(quantity),
		    image);
	}
	
	/**
	 * Returns a new AuctionSnapshot representing the given parameters
	 * @param product
	 * @param price
	 * @param reduction
	 * @param quantity
	 * @param image
	 */
	public AuctionSnapshot(String product, int price, int reduction, int quantity, String image) {
	  this(product, price, reduction, quantity, image, System.currentTimeMillis());
	}
	
	public AuctionSnapshot(String product, int price, int reduction,
	      int quantity, String image, long timestamp) {
	  this.product = product;
    this.price = price;
    this.reduction = reduction;
    this.quantity = quantity;
    this.image = image;
    this.timestamp = timestamp;
  }

  /*
	 * returns a 0 if the the given AuctionState has the same product and price
	 */
	public int compareTo(AuctionSnapshot other) {
		if (this.product.equals(other.product) && this.price == other.price) {
			return 0;
		} else if (this.timestamp > other.timestamp) {
			return 1;
		} else {
			return -1;
		}
	}

	public String toString() {
		return product + " $" + price + ", %" + reduction + " off, " + quantity + " left";
	}
}