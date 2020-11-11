package xyz.edward_p.slotbot.chippocket;

public class InsufficentChipException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 361588968706642455L;
	
	InsufficentChipException(String message) {
		super(message);
	}
}
