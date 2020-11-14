package xyz.edward_p.slotbot.games;

public class BetsNotSetException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6615822743834689318L;
	
	public BetsNotSetException(String message) {
		super(message);
	}
}
