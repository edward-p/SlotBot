package xyz.edward_p.slotbot;

import org.junit.Test;

public class SlotMachineTest {
    /**
     * Test the expectation of Payout
     */
    @Test
    public void PayoutExpectation() {
        double exp = 0.0;
        for (int i = 1; i < 65; i++) {
            exp += new SlotMachine(i).getPayoutRatio() / 64.0;
        }
        System.out.println("Expectation: " + exp);
    }

    @Test
    public void ParseParts() {
        //0b 00 00 00 (Bar, Bar, Berries)
        System.out.println(new SlotMachine(1));
        //0b 01 01 01 (Berries, Berries, Berries)
        System.out.println(new SlotMachine(0b010101 + 1));
        //0b 10 10 10 (Lemon, Lemon, Lemon)
        System.out.println(new SlotMachine(0b101010 + 1));
        //0b 11 11 11 (Seven, Seven, Seven)
        System.out.println(new SlotMachine(0b111111 + 1));
    }
}
