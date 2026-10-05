package se.miun.dt175g.octi.client.exceptions;

import java.io.IOException;

import se.miun.dt175g.octi.core.communicator.PlayerSetup;



//DON'T CHANGE!
public class OctiGameNoGameToParticipateException extends OctiGameSetupException {

	private static final long serialVersionUID = 1L;

	private final PlayerSetup setup;

	private final IOException ioException;

	public OctiGameNoGameToParticipateException(PlayerSetup setup) {
		this.setup = setup;
		ioException = null;
	}

	public OctiGameNoGameToParticipateException(IOException e) {
		ioException = e;
		setup = null;
	}

	@Override
	public String getMessage() {
		String message;
		if (setup != null)
			message = "Have you made sure that the server is up running on "
					+ setup.serverIpAddress + " port " + setup.gamePort
					+ " and that a new game has been started?";
		else
			message = ioException.getMessage();
		return message;
	}
}