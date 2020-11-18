package xyz.edward_p.slotbot.chippocket;

public class AlreadyInGameException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AlreadyInGameException(String message) {
		super(message);
	}
}
