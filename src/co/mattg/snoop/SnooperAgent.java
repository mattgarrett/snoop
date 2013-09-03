package co.mattg.snoop;

import org.codemonkey.simplejavamail.Email;
import org.codemonkey.simplejavamail.Mailer;
import org.codemonkey.simplejavamail.TransportStrategy;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;
import javax.mail.Message.RecipientType;

/**
 * Snooper Agent
 * 
 * @author matt
 */
public class SnooperAgent implements Runnable {

  private AuctionWatcher auctionWatcher;
  
	private AuctionSnapshot knownAuctionInitial;
	private AuctionSnapshot knownAuctionFinal;
	private Properties config;
	private AuctionDatabase database;
	
	/**
	 * Creates a Snooper Agent
	 * 
	 * @param config The config for the SnooperAgent
	 * @param database The Database for the SnooperAgent
	 */
	public SnooperAgent(Properties config, AuctionDatabase database) {
		this.config = config;
		this.database = database;
		this.auctionWatcher = new AuctionWatcher();
	}

	/**
	 * Runs the SnooperAgent
	 */
	public void run() {
		int sleep = Integer.parseInt(config.getProperty(SnooperMain.PING_FREQUENCY_KEY));
		while(true) {
			AuctionSnapshot liveAuction = auctionWatcher.getAuctionSnapshot();
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
					//TODO: maybe catch the IllegalArgumentException here?
					database.addIfNewAuctionSnapshot(knownAuctionInitial);
					knownAuctionInitial = liveAuction;
					knownAuctionFinal = liveAuction;
					alertOnNewAuction(liveAuction);
				}
			}
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
	public void alertOnNewAuction(AuctionSnapshot auction) {
		long lastSeen = database.getTimeStamp(auction);
		int hours = Integer.parseInt(config.getProperty(SnooperMain.COUNT_AS_NEW_KEY));
		if (lastSeen == -1 ||
			System.currentTimeMillis() - lastSeen > (hours * 1000 * 60 * 60)) {

			sendEmail(auction);
		}
	}
	
	/*
	 * Sends an email to the target, alerting them of the current auction
	 */
	public void sendEmail(AuctionSnapshot auction) {
		final Email email = new Email();
		email.setFromAddress(config.getProperty(SnooperMain.AGENT_NAME_KEY),
							 config.getProperty(SnooperMain.EMAIL_ADDRESS_KEY));
		email.setSubject("Steep and Cheap Alert!");
		email.addRecipient(config.getProperty(SnooperMain.TARGET_NAME_KEY),
						   config.getProperty(SnooperMain.EMAIL_ADDRESS_TARGET_KEY),
						   RecipientType.BCC);
		email.setText(auction.toString() + "!");
		email.setTextHTML("<b>" + auction.toString() + "!</b><br>" + auction.image);
	
		//creates a custom output stream that does nothing so the Mailer can't write
		//to it.  I have no idea how to make the mailer stop writing to stdout.
		PrintStream console = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			public void write(int b) {
				//do nothing
			}
		}));
		new Mailer(config.getProperty(SnooperMain.SMTP_SERVER_KEY),
				   Integer.parseInt(config.getProperty(SnooperMain.SMTP_PORT_KEY)),
				   config.getProperty(SnooperMain.EMAIL_ADDRESS_KEY),
				   config.getProperty(SnooperMain.EMAIL_PASSWORD_KEY),
				   TransportStrategy.SMTP_SSL).sendMail(email);
		System.setOut(console);
	}
}
