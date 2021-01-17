package com.sait.cst.logging;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class Application {
	private static final String DEFAULT_LOG_DIRECTORY = "cst-logs";
	private static final int DEFAULT_CAPTURE_DURATION = -1;
	public static final String LOG_FILENAME_EXTENSION = "log";

	public static void main(String[] args) {
		if (args.length == 0) {
			printUsage("Error: Missing arguments; see usage below.");
		}

		String mode = args[0];

		// depending on mode (first argument), determine what to do.
		// - capture: call CaptureApplication after making sure required IP address argument is provided.
		// - query: call QueryApplication after making sure type argument is provided.
		//
		// if an unknown mode is provided, print usage instructions and exit. 
		if (mode.equals("capture")) {
			if (args.length < 2) {
				printUsage("Error: IP address is required for capture mode.");
			}

			// capture mode supports stopping the capture automatically after a certain
			// delay. if none provided, then capture will run until user exists program.
			int captureDurationInSeconds = DEFAULT_CAPTURE_DURATION;
			if (args.length == 3) {
				captureDurationInSeconds = Integer.parseInt(args[2]);
			}

			CaptureApplication.run(args[1], DEFAULT_LOG_DIRECTORY, captureDurationInSeconds);
		} else if (mode.equals("query")) {
			if (args.length < 2) {
				printUsage("Error: type is required for capture mode.");
			}
			
			Instant startDate = null;
			Instant endDate = null;

			if (args.length > 2) {
				if (args.length < 4) {
					printUsage("Error: start and end times must to be specified together.");
				}

				startDate = parseDate(args[2]);
				if (startDate == null) {
					System.out.println("Warning: Failed to parse start date provided, defaulting to null.");
				}

				endDate = parseDate(args[3]);
				if (endDate == null) {
					System.out.println("Warning: Failed to parse end date provided, defaulting to null.");
				}
			}

			QueryApplication.run(DEFAULT_LOG_DIRECTORY, args[1], startDate, endDate);
		} else {
			printUsage("Error: Unknown mode provided; see usage below.");
		}
	}
	
	private static Instant parseDate(String dateString) {
		Instant instant;
		
		try {
			instant = Instant.parse(dateString);
		} catch (DateTimeParseException exception) {
			instant = null;
		}
		
		return instant;
	}

	private static void printUsage(String errorMessage) {
		if (errorMessage != null) {
			System.out.println(errorMessage);
			System.out.println();
		}

		System.out.println("Usage:");
		System.out.println("  cst-logs capture [IP ADDRESS] [duration in seconds]");
		System.out.println("  cst-logs query [TYPE] [start] [end]");
		System.out.println();
		System.out.println("Lowercase names for options means it is optional.");

		if (errorMessage != null) {
			System.exit(-1);
		}
	}
}
