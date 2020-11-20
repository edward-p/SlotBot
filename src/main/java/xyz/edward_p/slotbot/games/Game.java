package xyz.edward_p.slotbot.games;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import xyz.edward_p.slotbot.chippocket.ChipPocket;
import xyz.edward_p.slotbot.chippocket.AlreadyInGameException;

public abstract class Game implements Runnable {
	private static final int CHECK_UPDATES_INTERVAL = 200;
	protected boolean isRunning;
	protected TelegramBot bot;
	protected long chatId;
	// FIFO to store updates
	protected LinkedList<Update> updates;
	// All players
	protected Hashtable<Integer, ChipPocket> userDatas;
	// Joined players
	protected HashMap<Integer, Message> playerMessages;
	// gameOwner
	protected int owner;

	protected OnExitCallback callback;

	protected abstract void onUpdate(Update update);

	@Override
	public void run() {

		while (!Thread.interrupted()) {
			Update update = popUpdates();
			if (update != null) {
				onUpdate(update);
			}
			try {
				Thread.sleep(CHECK_UPDATES_INTERVAL);
			} catch (InterruptedException e) {
				break;
			}
		}
		// call back to remove current game from SlotBot.games
		callback.execute();

//		// debug
//		System.out.println("game ended");
	}

	protected Game(TelegramBot bot, long chatId, Hashtable<Integer, ChipPocket> userDatas) {
		this.isRunning = false;
		this.bot = bot;
		this.chatId = chatId;
		this.userDatas = userDatas;
		this.updates = new LinkedList<Update>();
		this.playerMessages = new HashMap<Integer, Message>();
	}

	public synchronized void addUpdates(Update update) {
		updates.add(update);
	}

	protected synchronized Update popUpdates() {
		return updates.size() > 0 ? updates.pop() : null;
	}

	protected ChipPocket getPocketByUserId(int userId) {
		ChipPocket pocket = null;
		if (userDatas.containsKey(userId)) {
			pocket = userDatas.get(userId);
		} else {
			pocket = new ChipPocket();
			userDatas.put(userId, pocket);
		}
		return pocket;
	}

	protected void addPlayer(Integer userId, Message message) throws AlreadyInGameException {
		ChipPocket pocket = getPocketByUserId(userId);
		if (pocket.getBets() == 0) {
			throw new BetsNotSetException("Player: " + userId);
		}
		if (pocket.isInGame()) {
			throw new AlreadyInGameException("Player: " + userId);
		}
		// set player status
		pocket.setCurrentGame(message.chat().id());
		pocket.setInGame(true);
		playerMessages.put(userId, message);
	}

	protected Message removePlayer(Integer userId) {
		ChipPocket pocket = getPocketByUserId(userId);
		// set player status
		pocket.setInGame(false);
		return playerMessages.remove(userId);
	}

	protected void replyJoined(long chatId, int messageId) {
		SendMessage message = new SendMessage(chatId, "已加入游戏");
		message.replyToMessageId(messageId);
		message.replyMarkup(new InlineKeyboardMarkup(new InlineKeyboardButton("下注").switchInlineQueryCurrentChat("")));

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

	protected void sendText(long chatId, String text) {
		SendMessage message = new SendMessage(chatId, text);

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

	protected void sendText(long chatId, int messageId, String text) {
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

	public Thread start(OnExitCallback callback) {
		if (!isRunning) {
			this.callback = callback;
			Thread th = new Thread(this);
			th.start();
			isRunning = true;
			return th;
//			// debug
//			System.out.println("game started");
		}
		return Thread.currentThread();
	}

	/**
	 * call this method if you want end your game CAN BE ONLY CALLED OR SUBCALLED BY
	 * run()
	 */
	protected void interrupt() {
		Thread.currentThread().interrupt();
	}

}
