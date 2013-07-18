package co.mattg.snoop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Database {
	private Set<String> banWords;
	private List<Auction> database;
	
	private Set<String> banWords() {
		Set<String> banWords = new HashSet<String>();
		banWords.add("Women's");
		banWords.add("Boy's");
		banWords.add("Girl's");
		banWords.add("Tent");
		banWords.add("Pole");
		return banWords;
	}
	
	/*
	 * Returns a new database that is initially empty
	 */
	public Database() {
		banWords = banWords();
		database = new LinkedList<Auction>();
	}
	
	/*
	 * Returns a new database that has been read from the given
	 * filename, throws an IOException if the file cannot be read
	 */
	public Database(String filename) throws IOException {
		banWords = banWords();
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
										 in.readLong(),
										 in.readLong()));
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
			//TODO: investigate the fact that this failed.
			//can a quantity actually go up?
			//finish.quantity > start.quantity ||
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
	public long getTimeStamp(AuctionState liveAuction) {
		for (String s : banWords) {
			if (liveAuction.product.contains(s)) {
				return System.currentTimeMillis();
			}
		}
		
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
	/*
	 * writes the database to the given file name, capping the database to
	 * items that were at most 'max' hours in the past.
	 */
	public void writeToFile(String filename, int max) throws IOException {
		DataOutputStream out = null;
		int current = (int) System.currentTimeMillis();
		int writeCount = 0;
		max = max * 60 * 60 * 1000; //convert max to ms;
		for (Auction auction : database) {
			if (current - auction.timestampStart > max) {
				break;
			}
			writeCount++;
		}
		
		try {
			out = new DataOutputStream(new FileOutputStream(filename));
			out.writeInt(writeCount);
			
			int written = 0;
			for (Auction auction : database) {
				if (written < writeCount) {
					out.writeUTF(auction.product);
					out.writeInt(auction.price);
					out.writeInt(auction.reduction);
					out.writeInt(auction.quantityStart);
					out.writeInt(auction.quantityFinish);
					out.writeLong(auction.timestampStart);
					out.writeLong(auction.timestampFinish);
				} else {
					break;
				}
				written++;
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
