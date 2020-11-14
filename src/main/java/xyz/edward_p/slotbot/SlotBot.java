package xyz.edward_p.slotbot;

import java.util.HashMap;
import java.util.Hashtable;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Dice;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.pengrad.telegrambot.response.SendResponse;

import xyz.edward_p.slotbot.chippocket.ChipPocket;
import xyz.edward_p.slotbot.chippocket.GetBonusTooOftenException;
import xyz.edward_p.slotbot.chippocket.InsufficentChipException;
import xyz.edward_p.slotbot.chippocket.NotEnoughAmountException;
import xyz.edward_p.slotbot.chippocket.PlayerAlreadyInGameException;
import xyz.edward_p.slotbot.chippocket.TooMuchChipsException;
import xyz.edward_p.slotbot.games.Game;
import xyz.edward_p.slotbot.games.SlotGame;

public class SlotBot {
	private static TelegramBot bot;
	private static String botName;
	private static Hashtable<Integer, ChipPocket> userDatas;
	private static HashMap<Long, Game> games;

	public static TelegramBot getBot() {
		return bot;
	}

	public static String getBotName() {
		return botName;
	}

	static {
		// get bot from Main
		bot = Main.getBot();

		// get bot name
		GetMeResponse getMeResponse;
		GetMe getMe = new GetMe();
		System.out.println("Bot is getting me...");
		getMeResponse = bot.execute(getMe);
		while (!getMeResponse.isOk()) {
			sleep10Secs();
			getMeResponse = bot.execute(getMe);
		}
		botName = getMeResponse.user().username();

		games = new HashMap<Long, Game>();
		// get userDatas
		userDatas = UserData.getUserDatas();
	}

	public static void defaultCallback(Update update) {

		Message message = update.message();
		if (message == null)
			return;
		// ignore forwarded message
		if (message.forwardFrom() != null)
			return;
		// ignore message from bots
		if (message.from().isBot())
			return;

		boolean isPrivate = update.message().chat().type().equals(Type.Private);

		Dice dice = update.message().dice();
		if (isPrivate && dice != null) {
			// private slot machine
			switch (dice.emoji()) {
			case "🎰":
				betSlotMachineCallback(update);
				break;
			}
			// done
			return;
		}

		String text = update.message().text();
		if (text == null)
			return;
		String[] slices = text.split(" ");

		if (!isPrivate && !slices[0].matches("^/.*@" + botName)) {
			return;
		} else {
			slices[0] = slices[0].replaceAll("@" + botName, "");
		}

		// common
		switch (slices[0]) {
		case "/setbets":
			setbetsCallback(update, slices);
			return;
		case "/getbets":
			getbetsCallback(update);
			return;
		case "/bonus":
			bonusCallback(update);
			return;
		case "/balance":
			balanceCallback(update);
			return;
		case "/transfer":
			transferCallback(update, slices);
			return;
		}
		if (isPrivate) {
			// private-only features
			switch (slices[0]) {
			case "/start":
			case "/help":
				helpCallback(update);
				break;
			}
		} else {
			// group-only features
			switch (slices[0]) {
			case "/newgame":
				newGame(update);
				break;
			default:
				redirect(update);
				break;
			}
		}
	}

	private static void betSlotMachineCallback(Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		ChipPocket pocket = getPocketByUserId(userId);
		if (pocket.getBets() == 0) {
			return;
		}
		try {
			SlotMachine slot = new SlotMachine(update.message().dice().value());
			int payOut = pocket.payOut(slot.getPayoutRatio());
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
			sendText(chatId, messageId, text);
		} catch (InsufficentChipException e) {
			pocket.setBets(0);
			String text = "筹码不足, 未下注\n赌注已经被重置为 0\n通过 /setbets 设置赌注\n通过 /bonus 来获得每日奖励筹码";
			sendText(chatId, messageId, text);
		}
	}

	/**
	 * redirect update to specific game
	 * 
	 * @param update
	 */
	private static void redirect(Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		Game game = games.get(chatId);
		if (game == null) {
			sendText(chatId, messageId, "当前没有在进行的游戏, 使用 /newgame 创建一个新游戏");
			return;
		}
		game.addUpdates(update);
	}

	/**
	 * Start a new game, use HashMap<Long, Game> games to sore (chatId, game) use
	 * .start() to start the game thread with a callback method witch removes game
	 * instance from games on thread exit
	 * 
	 * @param update
	 */
	private static void newGame(Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		if (games.containsKey(chatId)) {
			String text = "当前游戏还在进行中!";
			sendText(chatId, messageId, text);
			return;
		}
		Game game = new SlotGame(bot, userDatas);
		game.addUpdates(update);

		game.start(() -> {
			// remove game on thread exit
			games.remove(chatId);
		});
		games.put(chatId, game);
	}

	private static void helpCallback(Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();

		String text = "使用 /setbets 设置赌注\n发送 🎰 进行一次游戏";
		sendText(chatId, messageId, text);
	}

	private static void transferCallback(Update update, String[] args) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int sourceUserId = update.message().from().id();

		if (args.length < 2) {
			return;
		}

		Message replyToMessage = update.message().replyToMessage();
		if (replyToMessage == null) {
			String text = "请回复要转帐的用户的消息！";
			sendText(chatId, messageId, text);
			return;
		} else if (replyToMessage.from().isBot()) {
			String text = "不支持转帐给 Bot！";
			sendText(chatId, messageId, text);
			return;
		} else if (replyToMessage.from().id() == sourceUserId) {
			String text = "不支持转帐给自己！";
			sendText(chatId, messageId, text);
			return;
		}

		ChipPocket sourcePocket = getPocketByUserId(sourceUserId);
		ChipPocket targetPocket = getPocketByUserId(replyToMessage.from().id());

		int amount = -1;
		try {
			amount = Integer.parseInt(args[1]);
			sourcePocket.transferTo(amount, targetPocket);
			String text = "转帐成功!\n当前账户: " + sourcePocket.getBalance();
			sendText(chatId, messageId, text);
		} catch (InsufficentChipException e) {
			String text = "筹码不足, 转帐失败\n当前账户: " + sourcePocket.getBalance();
			sendText(chatId, messageId, text);
		} catch (NotEnoughAmountException e) {
			String text = "转帐数量太小, 最小: " + ChipPocket.MINIMUM_TRANSFER_AMOUNT;
			sendText(chatId, messageId, text);
		} catch (NumberFormatException e) {
			String text = "转帐数量必须为整数, 且最小为: " + ChipPocket.MINIMUM_TRANSFER_AMOUNT;
			sendText(chatId, messageId, text);
		}

	}

	private static void getbetsCallback(Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		ChipPocket pocket = getPocketByUserId(userId);
		String text = "当前赌注: " + pocket.getBets();

		sendText(chatId, messageId, text);
	}

	private static void balanceCallback(Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		ChipPocket pocket = getPocketByUserId(userId);

		String text = "当前账户: " + pocket.getBalance();
		sendText(chatId, messageId, text);
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

	private static void sendText(long chatId, int messageId, String text) {
		SendMessage message = new SendMessage(chatId, text);
		message.replyToMessageId(messageId);

		SendResponse response = bot.execute(message);
		while (!response.isOk()) {
			sleep10Secs();
			response = bot.execute(message);
		}
	}

	private static void bonusCallback(Update update) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		ChipPocket pocket = getPocketByUserId(userId);
		try {
			pocket.getBonus();
			String text = "签到成功，获得: " + ChipPocket.BONUS + "个筹码,\n当前账户: " + pocket.getBalance();
			sendText(chatId, messageId, text);
		} catch (GetBonusTooOftenException e) {
			long timeLeft = ChipPocket.BONUS_CD - (System.currentTimeMillis() - pocket.getLastTimeGetBonus());
			String text = "还需等待: " + (timeLeft / 1000.0) + " 秒";
			sendText(chatId, messageId, text);
		}
	}

	private static void setbetsCallback(Update update, String[] args) {
		long chatId = update.message().chat().id();
		int messageId = update.message().messageId();
		int userId = update.message().from().id();

		if (args.length < 2) {
			String text = "请给出下注数量！";
			sendText(chatId, messageId, text);
			return;
		}
		ChipPocket pocket = getPocketByUserId(userId);
		try {
			int bets = Integer.parseInt(args[1]);
			pocket.setBets(bets);
			String text = "设置成功, 当前赌注: " + pocket.getBets();
			sendText(chatId, messageId, text);
		} catch (NumberFormatException e) {
			String text = String.format("下注值必须为整数, 范围: [%d, %d]", ChipPocket.MINIMUM_BETS, ChipPocket.MAXIMUM_BETS);
			sendText(chatId, messageId, text);
		} catch (PlayerAlreadyInGameException e) {
			String text = "当前玩家正在游戏中，暂时不能设置赌注";
			sendText(chatId, messageId, text);
		} catch (InsufficentChipException e) {
			String text = "筹码不足, 设置失败\n当前账户: " + pocket.getBalance() + "\n或设置为 0, 表示不下注";
			sendText(chatId, messageId, text);
		} catch (NotEnoughAmountException e) {
			String text = "设置下注失败, 未达下限:" + ChipPocket.MINIMUM_BETS + "\n允许设置为 0, 表示不下注";
			sendText(chatId, messageId, text);
		} catch (TooMuchChipsException e) {
			String text = "设置下注失败, 超过上限:" + ChipPocket.MAXIMUM_BETS;
			sendText(chatId, messageId, text);
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
}
