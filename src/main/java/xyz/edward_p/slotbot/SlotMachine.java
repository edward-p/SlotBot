package xyz.edward_p.slotbot;

import xyz.edward_p.slotbot.chippocket.UnsupportedValueException;

public class SlotMachine {
	public final static int PART_BAR = 0;
	public final static int PART_BERRIES = 1;
	public final static int PART_LEMON = 2;
	public final static int PART_SEVEN = 3;
	public final static int PART_INDEX_SIZE = 3;
//	private final int value;
	private final int[] partIndexs;
	private int payOutRatio;

	public SlotMachine(int value) {
		if (value < 1 || value > 64) {
			throw new UnsupportedValueException("Value: " + value);
		}
//		this.value = value;

		partIndexs = new int[PART_INDEX_SIZE];
		for (int i = 0; i < partIndexs.length; i++) {
			partIndexs[i] = (value - 1) >> (i * 2) & 0b11;
		}

		/*
		 * calculating payout ratio (current expectation is 1)
		 */

		// for each kind of pattern
		for (int i = 0; i < 4; i++) {
			// check if 3-in-a-row
			int j;
			for (j = 0; j < partIndexs.length - 1; j++) {
				if (partIndexs[j] != partIndexs[j + 1]) {
					break;
				}
			}
			if (j == partIndexs.length - 1) {
				// 3-in-a-row
				// jackpot!
				switch (partIndexs[0]) {
					case PART_BAR -> {
						payOutRatio = 5;
						return;
					}
					case PART_BERRIES -> {
						payOutRatio = 15;
						return;
					}
					case PART_LEMON -> {
						payOutRatio = 20;
						return;
					}
					case PART_SEVEN -> {
						payOutRatio = 24;
						return;
					}
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
		return switch (partIndexs[index]) {
			case PART_BAR -> "Bar";
			case PART_BERRIES -> "Berries";
			case PART_LEMON -> "Lemon";
			case PART_SEVEN -> "Seven";
			default -> throw new IllegalArgumentException("Unexpected value: " + partIndexs[index]);
		};
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < partIndexs.length; i++) {
			sb.append(getPart(i));
			if (i != partIndexs.length - 1) {
				sb.append(", ");
			}
		}
		sb.append("], ");
		sb.append("Payout: ").append(getPayoutRatio());
		return sb.toString();
	}

//	public int getValue() {
//		return value;
//	}

	public int getPartIndex(int index) {
		return partIndexs[index];
	}
}
