package xyz.edward_p.slotbot;

import xyz.edward_p.slotbot.chippocket.UnsupportedValueException;

public class SlotMachine {
	private int value;
	private final static int PART_BAR = 0;
	private final static int PART_BERRIES = 1;
	private final static int PART_LEMON = 2;
	private final static int PART_SEVEN = 3;
	private final static int PART_INDEX_SIZE = 3;
	private int[] partIndex;
	private int payOutRatio;

	public SlotMachine(int value) {
		if (value < 1 || value > 64) {
			throw new UnsupportedValueException("Value: " + value);
		}
		this.value = value;

		partIndex = new int[PART_INDEX_SIZE];
		for (int i = 0; i < partIndex.length; i++) {
			partIndex[i] = (value - 1) >> (i * 2) & 0b11;
		}

		/*
		 * calculating payout ratio (current expectation is 1)
		 */
		
		// for each kind of pattern
		for (int i = 0; i < 4; i++) {
			// check if 3-in-a-row
			int j;
			for (j = 0; j < partIndex.length - 1; j++) {
				if (partIndex[j] != partIndex[j + 1]) {
					break;
				}
			}
			if (j == partIndex.length - 1) {
				// 3-in-a-row
				switch (partIndex[0]) {
				case PART_BAR:
					payOutRatio = 5;
					return;
				case PART_BERRIES:
					payOutRatio = 15;
					return;
				case PART_LEMON:
					payOutRatio = 20;
					return;
				case PART_SEVEN:
					// jackpot!
					payOutRatio = 24;
					return;
				}
			}
		}

//		// count Sevens, each Seven values 2 in payout ratio
//		int count = 0;
//		for (int i = 0; i < partIndex.length; i++) {
//			if (partIndex[i] == PART_SEVEN) {
//				count++;
//			}
//		}
//		if (count >= 2) {
//			payOutRatio = 2;
//		}

	}

	public int getPayoutRatio() {
		return payOutRatio;
	}

	private String getPart(int index) {
		switch (partIndex[index]) {
		case PART_BAR:
			return "Bar";
		case PART_BERRIES:
			return "Berries";
		case PART_LEMON:
			return "Lemon";
		case PART_SEVEN:
			return "Seven";
		default:
			throw new IllegalArgumentException("Unexpected value: " + partIndex[index]);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < partIndex.length; i++) {
			sb.append(getPart(i));
			if (i != partIndex.length - 1) {
				sb.append(", ");
			}
		}
		sb.append("]");
		sb.append("Payout: ").append(getPayoutRatio());
		return sb.toString();
	}

	public int getValue() {
		return value;
	}

}
