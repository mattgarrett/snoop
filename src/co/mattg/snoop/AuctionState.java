package co.mattg.snoop;

public class AuctionState implements Comparable<AuctionState>{
	public String product;
	public int price;
	public int reduction;
	public int quantity;
	public int timestamp;
	public String image;
	
	/*
	 * Returns a AuctionState representing the given product, price, discount, and quantity
	 * the image is an HTML chunk representing the product's image on the web
	 */
	public AuctionState(String product, String price, String reduction, String quantity, String image) {
		this.product = product.substring(0, product.length() - 1);
		this.price = Integer.parseInt(price.substring(1, price.length() - 3));
		this.reduction = Integer.parseInt(reduction.substring(0, reduction.length() - 5));
		this.quantity = Integer.parseInt(quantity);
		this.timestamp = (int) System.currentTimeMillis(); //will blow up eventually
		this.image = image;
	}
	
	/*
	 * returns a 0 if the the given AuctionState has the same product and price
	 */
	public int compareTo(AuctionState other) {
		if (this.product.equals(other.product) && this.price == other.price) {
			return 0;
		} else {
			return this.timestamp - other.timestamp;
		}
	}

	/*
	 * Returns a string representation of the AuctionState
	 */
	public String toString() {
		return product + " $" + price + ", %" + reduction + " off, " + quantity + " left";
	}
}