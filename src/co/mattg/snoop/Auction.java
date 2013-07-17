package co.mattg.snoop;

public class Auction {
	public String product;
	public int price;
	public int reduction;
	public int quantityStart;
	public int quantityFinish;
	public int timestampStart;
	public int timestampFinish;

	/*
	 * Returns a new auction representing the given product
	 */
	public Auction(String product,
				   int price,
				   int reduction,
				   int quantityStart,
				   int quantityFinish,
				   int timestampStart,
				   int timestampFinish) {
		
		this.product = product;
		this.price = price;
		this.quantityStart = quantityStart;
		this.quantityFinish = quantityFinish;
		this.timestampStart = timestampStart;
		this.timestampFinish = timestampFinish;
	}
}
