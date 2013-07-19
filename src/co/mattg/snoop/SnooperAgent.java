package co.mattg.snoop;

import org.codemonkey.simplejavamail.Email;
import org.codemonkey.simplejavamail.Mailer;
import org.codemonkey.simplejavamail.TransportStrategy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import javax.mail.Message.RecipientType;
import co.mattg.snoop.AuctionState;

public class SnooperAgent implements Runnable {
	public static final String URL = "http://www.steepandcheap.com/";
	public static final String PRODUCT = "product_title";
	public static final String PRICE = "price";
	public static final String REDUCTION = "percent_off";
	public static final String QUANTITY = "total_remaining";
	public static final String IMAGE = "item_image";
	
	private AuctionState knownAuctionInitial;
	private AuctionState knownAuctionFinal;
	private Properties config;
	private Database database;
	
	public SnooperAgent(Properties config, Database database) {
		this.config = config;
		this.database = database;
	}
	/*
	 * pings steep and cheap for auction information, e-mails the target
	 * when new auctions are found.
	 */
	public void run() {
		int sleep = Integer.parseInt(config.getProperty(SnooperMain.PING_FREQUENCY_KEY));
		int attempt = 0;
		for(;;) {
			AuctionState liveAuction = getCurrentAuctionState(attempt);
			if (liveAuction != null) {
				if (knownAuctionInitial == null) {
					knownAuctionInitial = liveAuction;
					knownAuctionFinal = liveAuction;
					//this will stop first time alerts
					//alertOnNewAuction(liveAuction);
				} else if (knownAuctionFinal.compareTo(liveAuction) == 0) {
					knownAuctionFinal = liveAuction;
				} else {
					//new item is live
					attempt = -1;
					//TODO: maybe catch the IllegalArgumentException here?
					database.add(knownAuctionInitial, knownAuctionFinal);
					knownAuctionInitial = liveAuction;
					knownAuctionFinal = liveAuction;
					alertOnNewAuction(liveAuction);
				}
			}
			attempt++;
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				return;
			}
		}
	}
	
	/*
	 * Decides whether the given AuctionState is new.  If it is considered new,
	 * prints to the console and emails the target.
	 */
	public void alertOnNewAuction(AuctionState auction) {
		long lastSeen = database.getTimeStamp(auction);
		int hours = Integer.parseInt(config.getProperty(SnooperMain.COUNT_AS_NEW_KEY));
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
	public void sendEmail(AuctionState auction) {
		final Email email = new Email();
		email.setFromAddress(config.getProperty(SnooperMain.AGENT_NAME_KEY),
							 config.getProperty(SnooperMain.EMAIL_ADDRESS_KEY));
		email.setSubject("Steep and Cheap Alert!");
		email.addRecipient(config.getProperty(SnooperMain.TARGET_NAME_KEY),
						   config.getProperty(SnooperMain.EMAIL_ADDRESS_TARGET_KEY),
						   RecipientType.BCC);
		email.setText(auction.toString() + "!");
		email.setTextHTML("<b>" + auction.toString() + "!</b><br>" + auction.image);
	
		//TODO: see if this nullifies the email output
		PrintStream console = System.out;
		System.setOut(null);
		new Mailer(config.getProperty(SnooperMain.SMTP_SERVER_KEY),
				   Integer.parseInt(config.getProperty(SnooperMain.SMTP_PORT_KEY)),
				   config.getProperty(SnooperMain.EMAIL_ADDRESS_KEY),
				   config.getProperty(SnooperMain.EMAIL_PASSWORD_KEY),
				   TransportStrategy.SMTP_SSL).sendMail(email);
		System.setOut(console);
	}
	
	/*
	 * returns an AuctionState representing the current item hosted on
	 * steep and cheap, or null if the server cannot be contacted
	 */
	private AuctionState getCurrentAuctionState(int attempt) {
		System.out.print("\rscrape attempt: " + attempt + " (type end to stop) ");
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
}
