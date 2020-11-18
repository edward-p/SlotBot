package xyz.edward_p.slotbot.chippocket;

import java.io.Serializable;

public class ChipPocket implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8997092531803445734L;
	public static final int MINIMUM_BETS = 10;
	public static final int MINIMUM_TRANSFER_AMOUNT = 10;
	public static final int MAXIMUM_BETS = 10000;
	public static final int BONUS = 10000;
	public static final int BONUS_CD = 360000;
	private long lastTimeGetBonus;
	private long balance;
	// if equals zero, means don't play
	private int bets;
	private transient boolean isInGame;
	private transient long currentGame;

	public ChipPocket() {
		lastTimeGetBonus = 0;
		balance = 0;
		bets = 0;
		isInGame = false;
	}

	public synchronized void getBonus() {
		if (System.currentTimeMillis() - lastTimeGetBonus < BONUS_CD)
			throw new GetBonusTooOftenException("Bonus is in CD.");
		balance += BONUS;
		lastTimeGetBonus = System.currentTimeMillis();
	}

	public long getBalance() {
		return balance;
	}

	public synchronized int payOut(int ratio) {
		if (bets > balance) {
			throw new InsufficentChipException("Balance: " + balance + ", Bets:" + bets);
		}

		balance -= bets;
		int ret = ratio * bets;
		if (ret > 0) {
			// if wined, get the bets back
			ret += bets;
			balance += ret;
		}
		return ret;
	}

	public synchronized void receiveTransfers(int amount) {
		balance += amount;
	}

	public synchronized void transferTo(int amount, ChipPocket chipPocket) {
		if (amount > balance) {
			throw new InsufficentChipException("Balance: " + balance + ", Transfers:" + amount);
		}
		if (amount < MINIMUM_TRANSFER_AMOUNT) {
			throw new NotEnoughAmountException("Transfers: " + bets + ", Minimum: " + MINIMUM_TRANSFER_AMOUNT);
		}
		balance -= amount;
		chipPocket.receiveTransfers(amount);
	}

	public int getBets() {
		return bets;
	}

	public synchronized void setBets(int bets) {
		if (bets != 0 && bets < MINIMUM_BETS) {
			throw new NotEnoughAmountException("Bets: " + bets + ", Minimum: " + MINIMUM_BETS);
		}
		if (bets > MAXIMUM_BETS) {
			throw new TooMuchChipsException("Bets: " + bets + ", Maximum: " + MAXIMUM_BETS);
		}
		if (bets > balance) {
			throw new InsufficentChipException("Balance: " + balance + ", Transfers:" + bets);
		}
		if (isInGame) {
			throw new AlreadyInGameException("");
		}
		this.bets = bets;
	}

	public synchronized boolean isInGame() {
		return isInGame;
	}

	public synchronized void setInGame(boolean isInGame) {
		this.isInGame = isInGame;
	}

	public long getLastTimeGetBonus() {
		return lastTimeGetBonus;
	}

	public synchronized long getCurrentGame() {
		if (!isInGame) {
			throw new NotInGameException("currently not in game");
		}
		return currentGame;
	}

	public synchronized void setCurrentGame(long currentGame) {
		if (isInGame) {
			throw new AlreadyInGameException("Already in game:" + currentGame);
		}
		this.currentGame = currentGame;
	}
}
