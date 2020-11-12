package xyz.edward_p.slotbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

import org.checkerframework.checker.nullness.qual.PolyKeyFor;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Dice;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendDice;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.pengrad.telegrambot.response.SendResponse;

import xyz.edward_p.slotbot.chippocket.ChipPocket;
import xyz.edward_p.slotbot.chippocket.GetBonusTwiceException;
import xyz.edward_p.slotbot.chippocket.InsufficentChipException;
import xyz.edward_p.slotbot.chippocket.NotEnoughAmountException;
import xyz.edward_p.slotbot.chippocket.TooMuchChipsException;

public class SlotBot {

	private static Hashtable<Integer, ChipPocket> userDatas;

	private static String botName;

	private static void onExitCallback(String path) {
		java.io.File file = new java.io.File(path);
		java.io.File parent = new java.io.File(file.getParent());
		if (!parent.exists()) {
			parent.mkdirs();
		}

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			ObjectOutputStream oos = null;
			try {
				oos = new ObjectOutputStream(out);
				oos.writeObject(userDatas);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (oos != null)
					try {
						oos.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	private static void init(String path) {
		FileInputStream in = null;
		try {
			in = new FileInputStream(path);
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(in);
				Object obj = null;
				try {
					obj = ois.readObject();
					userDatas = (Hashtable<Integer, ChipPocket>) obj;
				} catch (ClassNotFoundException e) {
					// user data not found
					userDatas = new Hashtable<Integer, ChipPocket>();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (ois != null)
					try {
						ois.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		} catch (FileNotFoundException e) {
			// create new user hashtable
			userDatas = new Hashtable<Integer, ChipPocket>();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	private static void sleep10Secs() {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void defaultCallback(TelegramBot bot, Update update) {

		if (update.message() == null)
			return;

		if (update.message().from().isBot())
			return;

		Dice dice = update.message().dice();

		if (dice != null) {
			if (dice.emoji().equals("🎰")) {
				betCallback(bot, update);
			}
			return;
		}

		String content = update.message().text();
		if (content == null)
			return;
		String[] slices = content.split(" ");
//		String userName = update.message().from().username();
//		System.out.println(userName + ": " + content);

		boolean isPrivate = update.message().chat().type().equals(Type.Private);

		if (!isPrivate && !slices[0].matches("^/.*@" + botName)) {
			return;
		} else {
			slices[0] = slices[0].replaceAll("@" + botName, "");
		}

		switch (slices[0]) {
		case "/setbets":
			setbetsCallback(bot, update, slices);
			break;
		case "/getbets":
			getbetsCallback(bot, update);
			break;
		case "/bonus":
			bonusCallback(bot, update);
			break;
		case "/balance":
			balanceCallback(bot, update);
			break;
		case "/transfer":
			transferCallback(bot, update, slices);
			break;
		}

	}

	private static void transferCallback(TelegramBot bot, Update update, String[] args) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int sourceUserId = update.message().from().id();

		if (args.length < 2) {
			return;
		}

		Message replyToMessage = update.message().replyToMessage();
		if (replyToMessage == null) {
			String text = "请回复要转帐的用户的消息！";
			sendText(bot, chatId, messageId, text);
			return;
		} else if (replyToMessage.from().isBot()) {
			String text = "不支持转帐给 Bot！";
			sendText(bot, chatId, messageId, text);
			return;
		} else if (replyToMessage.from().id() == sourceUserId) {
			String text = "不支持转帐给自己！";
			sendText(bot, chatId, messageId, text);
			return;
		}

		ChipPocket sourcePocket = getPocketByUserId(sourceUserId);
		ChipPocket targetPocket = getPocketByUserId(replyToMessage.from().id());

		int amount = -1;
		try {
			amount = Integer.parseInt(args[1]);
			sourcePocket.transferTo(amount, targetPocket);
			String text = "转帐成功!\n当前账户: " + sourcePocket.getBalance();
			sendText(bot, chatId, messageId, text);
		} catch (InsufficentChipException e) {
			String text = "筹码不足, 转帐失败\n当前账户: " + sourcePocket.getBalance();
			sendText(bot, chatId, messageId, text);
		} catch (NotEnoughAmountException e) {
			String text = "转帐数量太小, 最小: " + ChipPocket.MINIMUM_TRANSFER_AMOUNT;
			sendText(bot, chatId, messageId, text);
		} catch (NumberFormatException e) {
			String text = "转帐数量必须为整数, 且最小为: " + ChipPocket.MINIMUM_TRANSFER_AMOUNT;
			sendText(bot, chatId, messageId, text);
		}

	}

	private static void getbetsCallback(TelegramBot bot, Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		ChipPocket pocket = getPocketByUserId(userId);
		String text = "当前赌注: " + pocket.getBets();

		sendText(bot, chatId, messageId, text);
	}

	private static void balanceCallback(TelegramBot bot, Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		ChipPocket pocket = getPocketByUserId(userId);

		String text = "当前账户: " + pocket.getBalance();
		sendText(bot, chatId, messageId, text);
	}

	private static ChipPocket getPocketByUserId(int userId) {
		ChipPocket pocket = null;
		if (userDatas.containsKey(userId)) {
			pocket = userDatas.get(userId);
		} else {
			pocket = new ChipPocket();
			userDatas.put(userId, pocket);
		}
		return pocket;
	}

	private static void sendText(TelegramBot bot, long chatId, int messageId, String text) {
		SendMessage message = new SendMessage(chatId, text);
		message.replyToMessageId(messageId);

		SendResponse response = bot.execute(message);
		while (!response.isOk()) {
			sleep10Secs();
			response = bot.execute(message);
		}
	}

	private static void bonusCallback(TelegramBot bot, Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		ChipPocket pocket = getPocketByUserId(userId);
		try {
			pocket.getBonus();
			String text = "签到成功，获得: " + ChipPocket.DAILY_BONUS + "个筹码,\n当前账户: " + pocket.getBalance();
			sendText(bot, chatId, messageId, text);
		} catch (GetBonusTwiceException e) {
			String text = "今天已经来过了，明天再来吧～";
			sendText(bot, chatId, messageId, text);
		}
	}

	private static void setbetsCallback(TelegramBot bot, Update update, String[] args) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		if (args.length < 2) {
			String text = "请给出下注数量！";
			sendText(bot, chatId, messageId, text);
			return;
		}
		ChipPocket pocket = getPocketByUserId(userId);
		try {
			int bets = Integer.parseInt(args[1]);
			pocket.setBets(bets);
			String text = "设置成功, 当前赌注: " + pocket.getBets();
			sendText(bot, chatId, messageId, text);
		} catch (NumberFormatException e) {
			String text = String.format("下注值必须为整数, 范围: [%d, %d]", ChipPocket.MINIMUM_BETS, ChipPocket.MAXIMUM_BETS);
			sendText(bot, chatId, messageId, text);
		} catch (InsufficentChipException e) {
			String text = "筹码不足, 设置失败\n当前账户: " + pocket.getBalance() + "\n或设置为 0, 表示不下注";
			sendText(bot, chatId, messageId, text);
		} catch (NotEnoughAmountException e) {
			String text = "设置下注失败, 未达下限:" + ChipPocket.MINIMUM_BETS + "\n允许设置为 0, 表示不下注";
			sendText(bot, chatId, messageId, text);
		} catch (TooMuchChipsException e) {
			String text = "设置下注失败, 超过上限:" + ChipPocket.MAXIMUM_BETS;
			sendText(bot, chatId, messageId, text);
		}
	}

	private static void betCallback(TelegramBot bot, Update update) {

		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		ChipPocket pocket = getPocketByUserId(userId);
		if (pocket.getBets() == 0) {
			return;
		}
		try {
			SlotMachine slot = new SlotMachine(update.message().dice().value());
			int payOut = pocket.payOut(slot);
			String text;
			if (payOut != 0) {
				text = "恭喜获得: " + payOut + "个筹码！\n当前账户: " + pocket.getBalance();
			} else {
				text = "下次好运~\n当前账户: " + pocket.getBalance();
				if (pocket.getBets() > pocket.getBalance()) {
					pocket.setBets(0);
					text += "\n当前赌注大于余额, 已被重置为 0\n使用 /setbets 重新设置赌注";
				}
			}
			// Will loop until success
			sendText(bot, chatId, messageId, text);
		} catch (InsufficentChipException e) {
			pocket.setBets(0);
			String text = "筹码不足, 未下注\n赌注已经被重置为 0\n通过 /setbets 设置赌注\n通过 /bonus 来获得每日奖励筹码";
			sendText(bot, chatId, messageId, text);
		}
	}

	private static void printUsage() {
		System.out.print("Usage:\n\t");
		System.out.println("java -jar SlotBot.jar <TOKEN> <DATA_FILE_PATH>");
	}

	public static void main(String[] args) {

		if (args.length < 2) {
			printUsage();
			return;
		}
		String token = args[0];
		if (!token.matches("[0-9]{9,10}:[a-zA-Z0-9_-]{35}")) {
			System.out.println("Invalid token: " + token);
			return;
		}
		String dataFilePath = args[1];

		init(dataFilePath);

		TelegramBot bot = new TelegramBot(token);

		GetMeResponse getMeResponse;
		GetMe getMe = new GetMe();
		System.out.println("Bot is getting me...");
		getMeResponse = bot.execute(getMe);
		while (!getMeResponse.isOk()) {
			sleep10Secs();
			getMeResponse = bot.execute(getMe);
		}

		botName = getMeResponse.user().username();

		bot.setUpdatesListener(updates -> {
			// ... process updates
			for (Update update : updates) {
				defaultCallback(bot, update);
			}
			// return id of last processed update or confirm them all
			return UpdatesListener.CONFIRMED_UPDATES_ALL;
		});

		System.out.println("Listener started.");

		// save data to file on exit
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				onExitCallback(dataFilePath);
			}
		});

	}

}
