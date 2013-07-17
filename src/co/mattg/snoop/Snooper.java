package co.mattg.snoop;

import org.codemonkey.simplejavamail.Email;
import org.codemonkey.simplejavamail.Mailer;
import org.codemonkey.simplejavamail.TransportStrategy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.mail.Message.RecipientType;

import co.mattg.snoop.AuctionState;

public class Snooper {
	public static final String CONFIG = "config.txt";
	public static final String DATABASE_KEY = "database";
	public static final String SMTP_SERVER_KEY = "smtp-server";
	public static final String SMTP_PORT_KEY = "smtp-port";
	public static final String EMAIL_ADDRESS_KEY = "email";
	public static final String EMAIL_PASSWORD_KEY = "password";
	public static final String EMAIL_ADDRESS_TARGET_KEY = "target";
	public static final String AGENT_NAME_KEY = "agent-name";
	public static final String TARGET_NAME_KEY = "target-name";
	public static final String COUNT_AS_NEW_KEY = "auction-timeout(hours)";
	public static final String PING_FREQUENCY_KEY = "ping-frequency(milliseconds)";
	
	public static final String URL = "http://www.steepandcheap.com/";
	public static final String PRODUCT = "product_title";
	public static final String PRICE = "price";
	public static final String REDUCTION = "percent_off";
	public static final String QUANTITY = "total_remaining";
	public static final String IMAGE = "item_image";
	
	private static AuctionState knownAuctionInitial;
	private static AuctionState knownAuctionFinal;
	private static Database database;
	private static Properties config;
	
	/*
	 * pings steep and cheap for auction information, e-mails the target
	 * when new auctions are found.
	 */
	public static void main(String[] args) throws InterruptedException {
		try {
			config = new Properties();
			config.load(new FileInputStream(CONFIG));
			if (!isValidProperties(config)) {
				System.out.println("config file: " + CONFIG + " doesn't have required properties");
				System.exit(1);
			}
		} catch (IOException e) {
			System.out.println("could not find config file: " + CONFIG);
			System.exit(2);
		}
		try {
			database = new Database(config.getProperty(DATABASE_KEY));
		} catch (IOException e) {
			System.out.println("Couldn't read from the database.");
			System.out.print("Continue with a new database? (y/n) ");
			Scanner console = new Scanner(System.in);
			String answer = console.nextLine();
			while (!answer.startsWith("y") &&
				   !answer.startsWith("n") &&
				   !answer.startsWith("Y") &&
				   !answer.startsWith("N")) {
				
				System.out.print("Please answer yes or no. (y/n) ");
				answer = console.nextLine();
			}
			console.close();
			if (answer.startsWith("n") || answer.startsWith("N")) {
				System.exit(3);
			} else {
				database = new Database();
			}
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(database)));
		
		int sleep = Integer.parseInt(config.getProperty(PING_FREQUENCY_KEY));
		int attempt = 0;
		for(;;) {
			AuctionState liveAuction = getCurrentAuctionState(attempt);
			if (liveAuction != null) {
				if (knownAuctionInitial == null) {
					knownAuctionInitial = liveAuction;
					knownAuctionFinal = liveAuction;
					alertOnNewAuction(liveAuction);
				} else if (knownAuctionFinal.compareTo(liveAuction) == 0) {
					knownAuctionFinal = liveAuction;
				} else {
					//new item is live
					attempt = -1;
					database.add(knownAuctionInitial, knownAuctionFinal);
					knownAuctionInitial = liveAuction;
					knownAuctionFinal = liveAuction;
					alertOnNewAuction(liveAuction);
				}
			}
			attempt++;
			Thread.sleep(sleep);
		}
	}
	
	/*
	 * Returns true if the given Properties contains all the
	 * keys needed for Snooper to run and if the int keys can
	 * be parsed, returns false otherwise
	 */
	public static boolean isValidProperties(Properties config) {
		Set<String> properties = new HashSet<String>();
		properties.add(DATABASE_KEY);
		properties.add(SMTP_SERVER_KEY);
		properties.add(SMTP_PORT_KEY);
		properties.add(EMAIL_ADDRESS_KEY);
		properties.add(EMAIL_PASSWORD_KEY);
		properties.add(EMAIL_ADDRESS_TARGET_KEY);
		properties.add(AGENT_NAME_KEY);
		properties.add(TARGET_NAME_KEY);
		properties.add(COUNT_AS_NEW_KEY);
		properties.add(PING_FREQUENCY_KEY);
		
		if (!config.stringPropertyNames().containsAll(properties)) {
			return false;
		}
		try {
			Integer.parseInt(config.getProperty(COUNT_AS_NEW_KEY));
			Integer.parseInt(config.getProperty(SMTP_PORT_KEY));
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/*
	 * Decides whether the given AuctionState is new.  If it is considered new,
	 * prints to the console and emails the target.
	 */
	public static void alertOnNewAuction(AuctionState auction) {
		int lastSeen = database.getTimeStamp(auction);
		if (auction == null) {
			System.out.println("Hello!");
		}
		int hours = Integer.parseInt(config.getProperty(COUNT_AS_NEW_KEY));
		if (lastSeen == -1 ||
			System.currentTimeMillis() - lastSeen > (hours * 1000 * 60 * 60)) {

			System.out.println("\nnew auction: " + auction.toString() + "!");
			sendEmail(auction);
		} else {
			System.err.println("\nold auction: " + auction.toString());
		}
	}
	
	/*
	 * Sends an email to the target, alerting them of the current auction
	 */
	public static void sendEmail(AuctionState auction) {
		final Email email = new Email();
		email.setFromAddress(config.getProperty(AGENT_NAME_KEY), config.getProperty(EMAIL_ADDRESS_KEY));
		email.setSubject("Steep and Cheap Alert!");
		email.addRecipient(config.getProperty(TARGET_NAME_KEY), config.getProperty(EMAIL_ADDRESS_TARGET_KEY), RecipientType.BCC);
		email.setText(auction.toString() + "!");
		email.setTextHTML("<b>" + auction.toString() + "!</b><br>" + auction.image);
		
		new Mailer(config.getProperty(SMTP_SERVER_KEY),
				   Integer.parseInt(config.getProperty(SMTP_PORT_KEY)),
				   config.getProperty(EMAIL_ADDRESS_KEY),
				   config.getProperty(EMAIL_PASSWORD_KEY),
				   TransportStrategy.SMTP_SSL).sendMail(email);
	}
	
	/*
	 * returns an AuctionState representing the current item hosted on
	 * steep and cheap, or null if the server cannot be contacted
	 */
	private static AuctionState getCurrentAuctionState(int attempt) {
		System.out.print("\rscrape attempt: " + attempt);
		try {
			Document doc = Jsoup.connect(URL).get();
			AuctionState current = new AuctionState(doc.getElementById(PRODUCT).text(),
													doc.getElementById(PRICE).text(),
													doc.getElementById(REDUCTION).text(),
													doc.getElementById(QUANTITY).text(),
													doc.getElementById(IMAGE).html());
			return current;
		} catch (IOException e) {
			return null;
		}
	}
	
	/*
	 * A shutdown hook, runs when the JVM cleans up
	 * 
	 * The only purpose of this is to write the database to disk
	 * 
	 * Will work if the process is ended with C^ but not if it is
	 * killed by the OS.
	 */
	
	public static class ShutdownHook implements Runnable {
		private Database database;
		
		public ShutdownHook(Database database) {
			this.database = database;
		}
		
		@Override
		public void run() {
			try {
				database.writeToFile(config.getProperty(DATABASE_KEY));
				System.out.println("\nDatabase written to file.");
			} catch (IOException e) {
				System.out.println("\nCouldn't write database to file.");
			}
		}
	}
}
