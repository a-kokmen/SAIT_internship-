package com.sait.cst.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;

public class MessageHandler extends WebSocketAdapter {
	private boolean ready = false;
	private BufferedWriter bufferedWriter;
	private static final SimpleDateFormat filenameDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

	/**
	 * Creates a new instance of LogMessageHandler using a path and IP address. IP address provided is
	 * used to generate a filename which will be used to write messages received from IP address via
	 * WebSocket interface.
	 *
	 * If everything goes well, a property called ready is set to true. This property is used to signal
	 * to callers that LogMessageHandler interface is ready to be used.
	 */
	public MessageHandler(String baseLoggingDirectoryPath, String ipAddress) {
		String filename = getFilename(ipAddress);

		try {
			this.bufferedWriter = FileUtils.createBufferedWriter(baseLoggingDirectoryPath, filename);
		} catch (IOException exception) {
			LoggingUtils.ERROR("Failed to open a file (%s) for writing in logging directory (%s): %s", filename, baseLoggingDirectoryPath, exception.getMessage());
		}

		// mark ready or not depending on whether bufferedWriter creation was successful or not.
		this.ready = this.bufferedWriter != null;
	}

	@Override
	public void onTextMessage(WebSocket websocket, String text) throws Exception {
		// this line prints the message received to the screen; it can be commented out to reduce noise
		// while running the program.
		LoggingUtils.DEBUG(text);

		// try and write message received from WebSocket to the file using BufferedWriter. we use LogLine
		// class to serialize is properly using a timestamp and text received from WebSocket.
		try {
			LogLine logLine = new LogLine(System.currentTimeMillis(), text);
			bufferedWriter.write(LogLine.serialize(logLine));
			bufferedWriter.newLine();
		} catch (IOException exception) {
			LoggingUtils.ERROR("Failed to write to line to log file: %s", exception.getMessage());
		}
	}

	@Override
	public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
		LoggingUtils.ERROR("Failed to receive message from WebSocket: %s", cause.getMessage());
	}

	/**
	 * Returns whether or not LogMessageHandler is ready to consume text messages from a WebSocket.
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * Gracefully gets ready to be terminated by closing the log file so contents can be fully written
	 * to it before application is terminated.
	 */
	public void close() {
		try {
			bufferedWriter.close();
		} catch (IOException exception) {
			LoggingUtils.ERROR("Failed to finish writing to log file: %s", exception.getMessage());
		}
	}

	/**
	 * Generates a filename given an IP address. This value will be used to create files where log lines
	 * will be written. It includes a date suffix to make it easier to identify when files were created
	 * and help with uniqueness (i.e. no duplicate files).
	 */
	private static String getFilename(String ipAddress) {
		String dateSuffix = filenameDateFormat.format(new Date());
		return String.format("%s_%s.%s", ipAddress, dateSuffix, Application.LOG_FILENAME_EXTENSION);
	}
}
