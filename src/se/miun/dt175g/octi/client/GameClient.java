package se.miun.dt175g.octi.client;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.miun.dt175g.octi.core.Agent;
import se.miun.dt175g.octi.core.OctiAction;
import se.miun.dt175g.octi.core.OctiJsonAdapter;
import se.miun.dt175g.octi.core.OctiState;
import se.miun.dt175g.octi.core.Player;
import se.miun.dt175g.octi.core.communicator.PlayerSetup;


//DON'T CHANGE!
public class GameClient {
	private static final Logger logger = Logger.getLogger(GameClient.class.getName());
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Agent studentAgent;
	private PlayerSetup setup;

	public GameClient(PlayerSetup setup, Agent studentAgent) {
		this.setup = setup;
		this.studentAgent = studentAgent;
	}

	public void startConnection() {
		try {
			clientSocket = new Socket(setup.serverIpAddress, setup.gamePort);
			//clientSocket.setSoTimeout(5000);  // Set read timeout to 5000 milliseconds
			out = new ObjectOutputStream(clientSocket.getOutputStream());
			in = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error during connection initialization", e);
		}
	}

	public void sendPlayerSetup(PlayerSetup setup) {
		try {
			out.writeObject(setup);
		} catch (EOFException e) {
			System.out.println("Server disconnected unexpectedly. Exiting.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String receiveStateAsString() {
		try {
			String jsonState = (String) in.readObject();
			return jsonState;
		} catch (EOFException e) {
			System.out.println("Server disconnected unexpectedly. Exiting.");
			return null;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void receiveAndSetPlayerInfo() {
		try {
			Player player = (Player) in.readObject();
			Player oppPlayer = (Player) in.readObject();
			long timeLimit = (Long) in.readObject();
			studentAgent.setPlayersAndTimeLimit(player, oppPlayer, timeLimit);
		} catch (EOFException e) {
			System.out.println("Server disconnected unexpectedly. Exiting.");
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void sendJsonAction(String action) {
		try {
			out.writeObject(action);
		} catch (EOFException e) {
			System.out.println("Server disconnected unexpectedly. Exiting.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stopConnection() {
		try {
			in.close();
			out.close();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void play() {
		this.startConnection();
		this.sendPlayerSetup(setup);
		this.receiveAndSetPlayerInfo();
		while (true) {
			try {
				String stateAsJson = this.receiveStateAsString();
				if (stateAsJson == null) {
					logger.log(Level.INFO, "Received null state. Exiting loop.");
					break;
				}
				OctiState state = OctiState.createStateFromJson(stateAsJson);
				OctiAction action = studentAgent.getNextMove(state);
				String actionJson = OctiJsonAdapter.octiActionToJson(action);
				this.sendJsonAction(actionJson);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "An exception occurred during play", e);
				break;
			}
		}
		this.stopConnection();
	}

}
