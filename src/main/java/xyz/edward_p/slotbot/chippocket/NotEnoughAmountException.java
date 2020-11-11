package xyz.edward_p.slotbot.chippocket;

public class NotEnoughAmountException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 186774617790865890L;
	
	public NotEnoughAmountException(String message) {
		super(message);
	}
}
