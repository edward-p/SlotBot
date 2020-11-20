package xyz.edward_p.slotbot.games;

import java.util.HashMap;
import java.util.Hashtable;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.ChosenInlineResult;
import com.pengrad.telegrambot.model.InlineQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineQueryResultArticle;
import com.pengrad.telegrambot.request.AnswerInlineQuery;
import com.pengrad.telegrambot.request.SendDice;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import xyz.edward_p.slotbot.SlotBot;
import xyz.edward_p.slotbot.SlotMachine;
import xyz.edward_p.slotbot.chippocket.ChipPocket;
import xyz.edward_p.slotbot.chippocket.InsufficentChipException;
import xyz.edward_p.slotbot.chippocket.AlreadyInGameException;

public class SlotGame extends Game {
	// store player bet contexts
	private HashMap<Integer, SlotGameContext> playerContexts;

	public SlotGame(TelegramBot bot, long chatId, Hashtable<Integer, ChipPocket> userDatas) {
		super(bot, chatId, userDatas);
		playerContexts = new HashMap<Integer, SlotGameContext>();
	}

	protected void help(long chatId, int messageId) {
		String text = "当前游戏: 🎰\n使用 /join 加入游戏\n使用 /leave 离开游戏\n使用 /roll 进行一次游戏";
		SendMessage message = new SendMessage(chatId, text);
		message.replyToMessageId(messageId);

		SendResponse response = bot.execute(message);
		while (!response.isOk() && !Thread.interrupted()) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				break;
			}
			response = bot.execute(message);
		}
	}

	@Override
	protected void onUpdate(Update update) {
		if (update.inlineQuery() != null || update.chosenInlineResult() != null) {
			// redirect to answerInline
			parseInline(update);
			return;
		}

		Message message = update.message();
		int messageId = message.messageId();
		int userId = message.from().id();

		String content = update.message().text();
		if (content == null)
			return;

		if (message.viaBot() != null && !message.text().matches("❌")) {
			// update user message
			playerMessages.put(userId, message);
			sendText(chatId, messageId, "买定离手!");
		}

		String[] slices = content.split(" ");
		slices[0] = slices[0].replaceAll("@" + SlotBot.getBotName(), "");

		switch (slices[0]) {
		case "/newgame":
			owner = userId;
			join(message);
			if (Thread.currentThread().isInterrupted()) {
				// failed to start the game
				return;
			}
		case "/help":
			help(chatId, messageId);
			break;
		case "/join":
			join(message);
			break;
		case "/leave":
			String text;
			if (removePlayer(userId) == null) {
				text = "当前不在游戏中";
			} else {
				text = "已离开游戏";
			}
			if (playerMessages.size() == 0 || userId == owner) {
				text += "\n行吧~ 游戏结束";
				sendText(chatId, messageId, text);
				// end the game
				interrupt();
				return;
			}
			sendText(chatId, messageId, text);
			break;
		case "/roll":
			if (userId != owner) {
				sendText(chatId, messageId, "只有当前游戏主人可以使用!");
				return;
			}
			roll(message);
			break;
		}

	}

	private void parseInline(Update update) {
		// parse chosenInline start
		ChosenInlineResult chosenInlineResult = update.chosenInlineResult();
		if (chosenInlineResult != null) {
			int userId = chosenInlineResult.from().id();
			switch (chosenInlineResult.resultId()) {
			case "bar":
				playerContexts.put(userId, SlotGameContext.Bar);
				break;
			case "berries":
				playerContexts.put(userId, SlotGameContext.Berries);
				break;
			case "lemon":
				playerContexts.put(userId, SlotGameContext.Lemon);
				break;
			case "seven":
				playerContexts.put(userId, SlotGameContext.Seven);
				break;
			}
			return;
		}
		// parse chosenInline end

		// parse inlineQuery start
		InlineQuery inline = update.inlineQuery();
		int userId = inline.from().id();
		if (playerContexts.get(userId) != null) {
			InlineQueryResultArticle r = new InlineQueryResultArticle("closed", "❌ 买定离手", "❌");
			bot.execute(new AnswerInlineQuery(inline.id(), r).cacheTime(1).isPersonal(true));
			return;
		}

		InlineQueryResultArticle r1 = new InlineQueryResultArticle("bar", "🅱 Bar - 4/1", "Bar!");
		InlineQueryResultArticle r2 = new InlineQueryResultArticle("berries", "🍇 Berries - 4/1", "Berries!");
		InlineQueryResultArticle r3 = new InlineQueryResultArticle("lemon", "🍋 Lemon - 4/1", "Lemon!");
		InlineQueryResultArticle r4 = new InlineQueryResultArticle("seven", "7️⃣ Seven - 4/1", "Seven!");

		bot.execute(new AnswerInlineQuery(inline.id(), r1, r2, r3, r4).cacheTime(1).isPersonal(true));
		// parse inlineQuery end
	}

	private void join(Message message) {
		int userId = message.from().id();
		long chatId = message.chat().id();
		int messageId = message.messageId();

		try {
			addPlayer(userId, message);
			replyJoined(chatId, messageId);
		} catch (BetsNotSetException e) {
			if (userId == owner) {
				sendText(chatId, messageId, "未设置赌注, 启动游戏失败！");
				interrupt();
				return;
			}
			String text = "未设置赌注，请先使用 /setbets 设置赌注再 /join";
			sendText(chatId, messageId, text);
		} catch (AlreadyInGameException e) {
			String text = "你已经在一个多人游戏中";
			sendText(chatId, text);
		}
	}

	private void roll(Message message) {
		long chatId = message.chat().id();

		// check if all players put bets in
		if (playerContexts.size() != playerMessages.size()) {
			int messageId = message.messageId();
			String text = "当前还有玩家没下注, 无法开始";
			sendText(chatId, messageId, text);
			return;
		}

		// send slot machine
		SendResponse response = null;
		SendDice dice = new SendDice(chatId).slotMachine();
		response = bot.execute(dice);
		while (!response.isOk()) {
			response = bot.execute(dice);
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				return;
			}
		}

		SlotMachine slot = new SlotMachine(response.message().dice().value());
		int lastPartIndex = slot.getPartIndex(2);
		// notify all players
		for (Integer p : playerMessages.keySet()) {
			ChipPocket pocket = getPocketByUserId(p);
			Message m = playerMessages.get(p);
			int messageId = m.messageId();
			SlotGameContext context = playerContexts.get(p);
			try {
				String text;
				if (lastPartIndex == context.ordinal()) {
					// player wins
					int payOut;
					if (slot.getPayoutRatio() == 0) {
						payOut = pocket.payOut(4);
					} else {
						payOut = pocket.payOut(4 * slot.getPayoutRatio());
					}
					text = "恭喜获得: " + payOut + " 个筹码！\n当前账户: " + pocket.getBalance();
				} else {
					// player loses
					pocket.payOut(0);
					text = "下次好运~\n当前账户: " + pocket.getBalance();
				}
				// Will loop until success
				sendText(chatId, messageId, text);
			} catch (InsufficentChipException e) {
				pocket.setBets(0);
				String text = "筹码不足, 未下注\n赌注已经被重置为 0\n通过 /setbets 设置赌注\n通过 /bonus 来获得每日奖励筹码";
				sendText(chatId, text);
			} finally {
				// set player status
				pocket.setInGame(false);
				if (pocket.getBets() > pocket.getBalance()) {
					pocket.setBets(0);
					String text = "当前赌注大于余额, 已被重置为 0\n使用 /setbets 重新设置赌注";
					sendText(chatId, text);
				}
			}
		}

		sendText(chatId, "游戏结束!");
		// end the game
		interrupt();
	}

}
