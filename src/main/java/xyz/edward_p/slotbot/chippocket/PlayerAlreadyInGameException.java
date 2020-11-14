package xyz.edward_p.slotbot.chippocket;

public class PlayerAlreadyInGameException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PlayerAlreadyInGameException(String message) {
		super(message);
	}
}
