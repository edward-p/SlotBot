package xyz.edward_p.slotbot.chippocket;

import java.io.Serializable;
import java.util.Calendar;

import xyz.edward_p.slotbot.SlotMachine;

public class ChipPocket implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8997092531803445734L;
	public static final int MINIMUM_BETS = 10;
	public static final int MINIMUM_TRANSFER_AMOUNT = 10;
	public static final int MAXIMUM_BETS = 1000;
	public static final int DAILY_BONUS = 1000;
	private Calendar lastDayGetBonus;
	private long balance;
	// if equals zero, means don't play
	private int bets;

	public ChipPocket() {
		lastDayGetBonus = Calendar.getInstance();
		lastDayGetBonus.set(1970, 1, 1);
		balance = 0;
		bets = 0;
	}

	public void getBonus() {
		Calendar calendar = Calendar.getInstance();
		int sum = calendar.get(Calendar.YEAR) - lastDayGetBonus.get(Calendar.YEAR);
		sum += calendar.get(Calendar.DAY_OF_YEAR) - lastDayGetBonus.get(Calendar.DAY_OF_YEAR);
		if (sum == 0)
			throw new GetBonusTwiceException("Get bonus twice in a day.");
		balance += DAILY_BONUS;
		lastDayGetBonus = calendar;
	}

	public long getBalance() {
		return balance;
	}

	public int payOut(SlotMachine slotmachine) {
		if (bets > balance) {
			throw new InsufficentChipException("Balance: " + balance + ", Bets:" + bets);
		}

		balance -= bets;
		int ret = slotmachine.getPayoutRatio() * bets;
		if (ret > 0) {
			// if wined, get the bets back
			ret += bets;
			balance += ret;
		}
		return ret;
	}

	public void receiveTransfers(int amount) {
		balance += amount;
	}

	public void transferTo(int amount, ChipPocket chipPocket) {
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

	public void setBets(int bets) {
		if (bets != 0 && bets < MINIMUM_BETS) {
			throw new NotEnoughAmountException("Bets: " + bets + ", Minimum: " + MINIMUM_BETS);
		}
		if (bets > MAXIMUM_BETS) {
			throw new TooMuchChipsException("Bets: " + bets + ", Maximum: " + MAXIMUM_BETS);
		}
		if (bets > balance) {
			throw new InsufficentChipException("Balance: " + balance + ", Transfers:" + bets);
		}
		this.bets = bets;
	}

}
