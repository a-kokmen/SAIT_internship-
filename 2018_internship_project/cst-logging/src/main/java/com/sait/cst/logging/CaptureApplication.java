package com.sait.cst.logging;

import java.util.Timer;
import java.util.TimerTask;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

public class CaptureApplication {
	private static final int CONNECTION_TIMEOUT = 5000;

	public static void run(String ipAddress, String logDirectory, int captureDurationInSeconds) {
		String socketAddress = String.format("ws://%s", ipAddress);

		// create directory where file(s) will be written. if creating directory fails for some
		// reason, exit the program.
		if (!FileUtils.createDirectory(logDirectory)) {
			LoggingUtils.ERROR("Failed to create directory: %s", logDirectory);
			System.exit(-2);
		}

		// call which will initiate capturing messages from IP address provided.
		captureMessages(logDirectory, ipAddress, socketAddress, captureDurationInSeconds);
	}

	/**
	 * Creates a WebSocket instance which will be used to receive messages using handler provided.
	 */
	private static WebSocket connect(String address, MessageHandler handler) throws Exception {
		return new WebSocketFactory()
				.setConnectionTimeout(CONNECTION_TIMEOUT)
				.createSocket(address)
				.addListener(handler)
				.connect();
	}

	/**
	 * Registering a shutdown hook helps with gracefully terminating the program when users quit from
	 * the command line using Ctrl + C. This is done to make sure program cleans up after itself properly
	 * by closing the WebSocket connection and letting handler finish writing to the file.
	 */
	private static void registerShutdownHook(final WebSocket socket, final MessageHandler handler) {
		Runtime.getRuntime().addShutdownHook(
				new Thread() {
		            @Override
		            public void run() {
		                LoggingUtils.INFO("Terminating.");

		                if (socket != null) {
		                	socket.disconnect();
		                }

		                if (handler != null) {
		                	handler.close();
		                }
		            }
		});
	}

	private static void registerShutdownTimer(int delayInSeconds) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// calling System.exit will trigger a graceful termination which will
				// make sure shutdown hooks registered (above) are executed.
				System.exit(0);
			}
		}, delayInSeconds * 1000);
	}

	/**
	 * This method does the heavy-lifting by creating a WebSocket and sending an initial message to it
	 * to start pulling information.
	 */
	private static void captureMessages(String baseLoggingDirectoryPath, String ipAddress, String socketAddress, int captureDurationInSeconds) {
		MessageHandler handler = new MessageHandler(baseLoggingDirectoryPath, ipAddress);

		if (!handler.isReady()) {
			LoggingUtils.ERROR("Failed to initialize LogMessageHandler successfully.");
			System.exit(-3);
		}

		try {
			final WebSocket socket = connect(socketAddress, handler);
			LoggingUtils.INFO("Connected to WebSocket (%s) successfully.", socketAddress);

			String message = String.format("init:[%d]", System.currentTimeMillis());
			LoggingUtils.DEBUG("Sending %s to device.", message); 
			socket.sendText(message);

			// this allows program to disconnect gracefully when user wants to stop.
			registerShutdownHook(socket, handler);

			// if a captureDuration is provided, then we will use it to set a timer that
			// will be used to terminate the program gracefully.
			if (captureDurationInSeconds > 0) {
				registerShutdownTimer(captureDurationInSeconds);
			}
		} catch (Exception exception) {
			LoggingUtils.ERROR("Failed to WebSocket (%s): (%s)", socketAddress, exception.getMessage());
		}
	}
}
