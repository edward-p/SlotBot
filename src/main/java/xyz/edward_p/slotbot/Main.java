package xyz.edward_p.slotbot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;

public class Main {
	private static TelegramBot bot;
	private static String dataFilePath;

	public static TelegramBot getBot() {
		return bot;
	}

	public static String getDataFilePath() {
		return dataFilePath;
	}

	private static void printUsage() {
		System.out.print("Usage:\n\t");
		System.out.println("java -jar SlotBot.jar <TOKEN> <DATA_FILE_PATH>");
	}

	public static void main(String[] args) {

		// need 2 args, token & data file
		if (args.length < 2) {
			printUsage();
			return;
		}
		String token = args[0];
		// check if token is a valid bot token
		if (!token.matches("[0-9]{9,10}:[a-zA-Z0-9_-]{35}")) {
			System.out.println("Invalid token: " + token);
			return;
		}

		dataFilePath = args[1];
		
		bot = new TelegramBot(token);
		bot.setUpdatesListener(updates -> {
			// ... process updates
			for (Update update : updates) {
				SlotBot.defaultCallback(update);
			}
			// return id of last processed update or confirm them all
			return UpdatesListener.CONFIRMED_UPDATES_ALL;
		});

		System.out.println("Listener started.");

		
		// save data to file on exit
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				UserData.writeOut(dataFilePath);
			}
		});

	}

}
