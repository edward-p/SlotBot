package xyz.edward_p.slotbot.chippocket;

public class GetBonusTooOftenException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7944548186485378284L;
	public GetBonusTooOftenException(String message) {
		super(message);
	}
}
