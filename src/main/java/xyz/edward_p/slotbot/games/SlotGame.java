package xyz.edward_p.slotbot.games;

import java.util.Hashtable;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendDice;
import com.pengrad.telegrambot.response.SendResponse;

import xyz.edward_p.slotbot.SlotBot;
import xyz.edward_p.slotbot.SlotMachine;
import xyz.edward_p.slotbot.chippocket.ChipPocket;
import xyz.edward_p.slotbot.chippocket.InsufficentChipException;
import xyz.edward_p.slotbot.chippocket.PlayerAlreadyInGameException;

public class SlotGame extends Game {

	public SlotGame(TelegramBot bot, Hashtable<Integer, ChipPocket> userDatas) {
		super(bot, userDatas);
	}

	protected void help(long chatId, int messageId) {
		String text = "当前游戏: 🎰\n使用 /join 加入游戏\n使用 /leave 离开游戏\n使用 /roll 进行一次游戏";
		sendText(chatId, messageId, text);
	}

	@Override
	protected void onUpdate(Update update) {
		Message message = update.message();
		long chatId = message.chat().id();
		int messageId = message.messageId();
		int userId = message.from().id();
		String userName = message.from().username();

		String content = update.message().text();
		if (content == null)
			return;
		String[] slices = content.split(" ");
		slices[0] = slices[0].replaceAll("@" + SlotBot.getBotName(), "");

		switch (slices[0]) {
		case "/newgame":
			owner = userId;
			join(chatId, userId, userName);
			if (Thread.currentThread().isInterrupted()) {
				// failed to start the game
				return;
			}
		case "/help":
			help(chatId, messageId);
			break;
		case "/join":
			join(chatId, userId, userName);
			break;
		case "/leave":
			String text;
			if (removePlayer(userId) == null) {
				text = "@" + userName + " 当前不在游戏中";
			} else {
				text = "@" + userName + " 已离开游戏";
			}
			if (players.size() == 0 || userId == owner) {
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
				String ownerName = players.get(owner);
				sendText(chatId, messageId, "只有当前游戏主人 @" + ownerName + " 可以使用!");
				return;
			}
			roll(message);
		}

	}

	private void join(long chatId, int userId, String userName) {
		try {
			addPlayer(userId, userName);
			String text = "@" + userName + " 已加入游戏";
			sendText(chatId, text);
		} catch (BetsNotSetException e) {
			if (userId == owner) {
				sendText(chatId, "游戏主人 @" + userName + " 未设置赌注, 启动游戏失败！");
				interrupt();
				return;
			}
			String text = "@" + userName + " 未设置赌注，请先使用 /setbets 设置赌注再 /join";
			sendText(chatId, text);
		} catch (PlayerAlreadyInGameException e) {
			String text = "@" + userName + " 已经在游戏中";
			sendText(chatId, text);
		}
	}

	private void roll(Message message) {
		long chatId = message.chat().id();
		int messageId = message.messageId();

		if (players.size() == 0) {
			String text = "当前玩家人数不足, 无法开始, 使用 /join 加入游戏";
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

		// notify all players
		for (Integer p : players.keySet()) {
			ChipPocket pocket = getPocketByUserId(p);
			String name = players.get(p);
			try {
				String text;
				int payOut = pocket.payOut(slot.getPayoutRatio());
				if (payOut != 0) {
					text = "恭喜 @" + name + " 获得: " + payOut + "个筹码！\n当前账户: " + pocket.getBalance();
				} else {
					text = "@" + name + " 下次好运~\n当前账户: " + pocket.getBalance();
				}
				// Will loop until success
				sendText(chatId, text);
			} catch (InsufficentChipException e) {
				pocket.setBets(0);
				String text = "@" + name + "筹码不足, 未下注\n赌注已经被重置为 0\n通过 /setbets 设置赌注\n通过 /bonus 来获得每日奖励筹码";
				sendText(chatId, text);
			} finally {
				// set player status
				pocket.setInGame(false);
				if (pocket.getBets() > pocket.getBalance()) {
					pocket.setBets(0);
					String text = "@" + name + "当前赌注大于余额, 已被重置为 0\n使用 /setbets 重新设置赌注";
					sendText(chatId, text);
				}
			}
		}

		sendText(chatId, messageId, "游戏结束!");
		// end the game
		interrupt();
	}

}
