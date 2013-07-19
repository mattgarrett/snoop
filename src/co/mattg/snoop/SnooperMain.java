package co.mattg.snoop;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

public class SnooperMain {
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
	
	private static Properties config;
	
	public static void main(String[] args) {
		config = getConfig();
		Database database = getDatabase();
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(database)));
		intro();
		
		Scanner console = new Scanner(System.in);
		int choice = options(console);
		while (choice != 4) {
			if (choice == 1) {
				Thread agent = new Thread(new SnooperAgent(config, database));
				agent.start();
				console.next();
				agent.interrupt();
				try {
					agent.join();
				} catch (InterruptedException e) {
					//Main thread should not be interrupted
				}
			} else if (choice == 2) {
				//TODO: Item selection and watchlist
			} else if (choice == 3) {
				//TODO: Information and help
			}
			choice = options(console);
		}
		console.close();
	}
	
	public static int options(Scanner console) {
		System.out.println();
		System.out.println("1.) Scan steepandcheap.com");
		System.out.println("2.) Pick items to watch");
		System.out.println("3.) Help and information");
		System.out.println("4.) Exit");
		System.out.print("Enter your choice: ");
		return console.nextInt();
	}
	
	public static Properties getConfig() {
		Properties config = null;
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
		System.out.println("...Config Loaded");
		return config;
	}
	
	public static Database getDatabase() {
		Database database = null;
		try {
			database = new Database(config.getProperty(DATABASE_KEY));
			System.out.println("...Database Loaded");
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
			}
			database = new Database();
			System.out.println("...Database Created");
		}
		return database;
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
	
	public static void intro() {
		System.out.println("\nWelcome to SteepAndCheap Snooper");
		System.out.println("We'll send you alerts about deals you care about");
	}
}
