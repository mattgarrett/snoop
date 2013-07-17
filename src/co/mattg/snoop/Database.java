package co.mattg.snoop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Database {
	private List<Auction> database;
	
	/*
	 * Returns a new database that is initially empty
	 */
	public Database() {
		database = new LinkedList<Auction>();
	}
	
	/*
	 * Returns a new database that has been read from the given
	 * filename, throws an IOException if the file cannot be read
	 */
	public Database(String filename) throws IOException {
		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(filename));
			int auctions = in.readInt();
			database = new LinkedList<Auction>();
			
			for (int i = 0; i < auctions; i++) {
				database.add(new Auction(in.readUTF(),
										 in.readInt(),
										 in.readInt(),
										 in.readInt(),
										 in.readInt(),
										 in.readInt(),
										 in.readInt()));
			}
		} catch (FileNotFoundException e) {
			throw new IOException();
		} catch (EOFException e) {
			throw new IOException();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
	
	/*
	 * Adds a new Auction to the database representing the given
	 * start and finish AuctionStates
	 */
	public void add(AuctionState start, AuctionState finish) {
		if (!start.product.equals(finish.product) ||
			start.price != finish.price ||
			start.reduction != finish.reduction ||
			finish.quantity > start.quantity ||
			start.timestamp > finish.timestamp) {
			
			//TODO: remove this
			System.err.println("Debug Info");
			System.err.println(start);
			System.err.println(finish);
			throw new IllegalArgumentException();
		}
		
		add(new Auction(start.product,
						start.price,
						start.reduction,
						start.quantity,
						finish.quantity,
						start.timestamp,
						finish.timestamp));
	}
	
	/*
	 * Adds the given Auction to the database
	 */
	public void add(Auction auction) {
		database.add(0, auction);
	}
	
	/*
	 * returns the timestamp of the most recent database entry
	 * for the product in this AuctionState or -1 if it is not
	 * in the database
	 */
	public int getTimeStamp(AuctionState liveAuction) {
		for (Auction auction : database) {
			if (auction.product.equals(liveAuction.product) &&
				auction.price == liveAuction.price) {
				return auction.timestampFinish;
			}
		}
		return -1;
	}
	
	/*
	 * Writes the database to the given file
	 */
	public void writeToFile(String filename) throws IOException{
		writeToFile(filename, Integer.MAX_VALUE);
	}
	
	//TODO: max should be changed to a timestamp or a window of time
	public void writeToFile(String filename, int max) throws IOException {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new FileOutputStream(filename));
			if (max > database.size()) {
				out.writeInt(database.size());
			} else {
				out.writeInt(max);
			}
			int count = 0;
			for (Auction auction : database) {
				if (count < max) {
					out.writeUTF(auction.product);
					out.writeInt(auction.price);
					out.writeInt(auction.reduction);
					out.writeInt(auction.quantityStart);
					out.writeInt(auction.quantityFinish);
					out.writeInt(auction.timestampStart);
					out.writeInt(auction.timestampFinish);
				} else {
					break;
				}
				count++;
			}
		} catch (FileNotFoundException e) {
			throw new IOException();
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
}
