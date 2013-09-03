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
import java.util.Set;

/**
 * A database of past AuctionSnapshots
 * 
 * @author matt
 */
public class AuctionDatabase {
  private LinkedList<AuctionSnapshot> auctionSnapshots;
  
  private Set<String> banWords;
	
	private Set<String> banWords() {
		Set<String> banWords = new HashSet<String>();
		banWords.add("Women's");
		banWords.add("Boy's");
		banWords.add("Girl's");
		banWords.add("Tent");
		banWords.add("Pole");
		banWords.add("Climbing Shoe");
		return banWords;
	}
	
	/**
	 * Creates a new, empty AuctionDatabase
	 */
	public AuctionDatabase() {
		banWords = banWords();
		auctionSnapshots = new LinkedList<AuctionSnapshot>();
	}
	
	/*
	 * Returns a new database that has been read from the given
	 * filename, throws an IOException if the file cannot be read
	 */
	
	/**
	 * Creates a new database from the file with the given path
	 * @param filePath the path to the database file
	 * @throws IOException if the file cannot be read
	 */
	public AuctionDatabase(String filePath) throws IOException {
		banWords = banWords();
		auctionSnapshots = new LinkedList<AuctionSnapshot>();
		
		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(filePath));
			int auctions = in.readInt();
			
			for (int i = 0; i < auctions; i++) {
				auctionSnapshots.add(new AuctionSnapshot(in.readUTF(),
										 in.readInt(),
										 in.readInt(),
										 in.readInt(),
										 null,
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

	/**
	 * Adds the given AuctionShapshot to the AuctionDatabase if
	 * the given AuctionSnapshot is different from the most recent
	 * AuctionSnapshot
	 * @param auctionSnapshot
	 * @return returns true if the AuctionShapshot was added false otherwise
	 */
	public boolean addIfNewAuctionSnapshot(AuctionSnapshot auctionSnapshot) {
	  AuctionSnapshot mostRecent = getMostRecentAuctionSnapshot();
	  if (mostRecent == null || !mostRecent.equals(auctionSnapshot)) {
	    auctionSnapshots.add(auctionSnapshot);
	    return true;
	  } else {
	    return false;
	  }
	}
	
	public AuctionSnapshot getMostRecentAuctionSnapshot() {
	  return (auctionSnapshots.isEmpty()) ? null : auctionSnapshots.get(auctionSnapshots.size() - 1);
	}
	
	/*
	 * returns the timestamp of the most recent database entry
	 * for the product in this AuctionState or -1 if it is not
	 * in the database
	 */
	public long getTimeStamp(AuctionSnapshot auctionSnapshot) {
		for (String s : banWords) {
			if (auctionSnapshot.product.contains(s)) {
				return System.currentTimeMillis();
			}
		}
		
		for (AuctionSnapshot databaseAuctionSnapshot : auctionSnapshots) {
			if (auctionSnapshot.product.equals(databaseAuctionSnapshot.product) &&
				auctionSnapshot.price == databaseAuctionSnapshot.price) {
				return databaseAuctionSnapshot.timestamp;
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
		//TODO(matt): these loops should be rolled into one
		max = max * 60 * 60 * 1000; //convert max to ms;
		for (AuctionSnapshot auctionSnapshot : auctionSnapshots) {
			if (current - auctionSnapshot.timestamp > max) {
				break;
			}
			writeCount++;
		}
		
		try {
			out = new DataOutputStream(new FileOutputStream(filename));
			out.writeInt(writeCount);
			
			int written = 0;
			for (AuctionSnapshot auctionSnapshot : auctionSnapshots) {
				if (written < writeCount) {
					out.writeUTF(auctionSnapshot.product);
					out.writeInt(auctionSnapshot.price);
					out.writeInt(auctionSnapshot.reduction);
					out.writeInt(auctionSnapshot.quantity);
					out.writeLong(auctionSnapshot.timestamp);
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
