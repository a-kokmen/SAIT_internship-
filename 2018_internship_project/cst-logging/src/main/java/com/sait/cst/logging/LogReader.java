package com.sait.cst.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogReader {
	private BufferedReader bufferedReader;

	/**
	 * Creates a LogReader instance given a file handle. This will internally create a BufferedReader
	 * so lines can be read from the file. If file doesn't exist, program will terminate.
	 */
	public LogReader(File file) {
		try {
			this.bufferedReader = FileUtils.createBufferedReader(file);
		} catch (FileNotFoundException exception) {
			LoggingUtils.ERROR("Failed to open file because it doesn't exist: %s", exception.getMessage());
			System.exit(-2);
		}
	}

	/**
	 * This method is used to get matching log lines given an expected type (e.g. fm, cc), a start timestamp
	 * and an endTimestamp. Method will iterate over the lines in the file and make perform checks to
	 * make sure conditions are met.
	 */
	public List<LogLine> getMatchingLogLines(String type, long startTimestamp, long endTimestamp) {
		LoggingUtils.DEBUG("Collecting matching logs for type=%s startTimestamp=%d endTimestamp=%d.", type, startTimestamp, endTimestamp);

		// initialize a list of LogLine instances which will be used to collect matching lines.
		List<LogLine> logLines = new ArrayList<>();

		try {
			String line;

			// until end of the file is reached, read line by line.
			// every line consists of a comma-separated value:
			// - timestamp: milliseconds since epoch. it can be converted into a date
			// - json: string which can be parsed as a JSON object for further analysis.
			while ((line = bufferedReader.readLine()) != null) {
				LogLine logLine = LogLine.deserailize(line);
				if (logLine == null) {
					LoggingUtils.ERROR("Failed to parse line as LogLine: %s", line);
					continue;
				}

				LoggingUtils.DEBUG("# %s", logLine);

				// if timestamp read is before start, then skip.
				if (logLine.isBefore(startTimestamp)) {
					continue;
				}

				// if timestamp read is after end, then stop processing.
				if (logLine.isAfter(endTimestamp)) {
					break;
				}
				
				// if logLine isn't expected type, then skip.
				if (!logLine.is(type)) {
					continue;
				}

				// if we made it this far, add it to matching list.
				logLines.add(logLine);
			}
		} catch (IOException exception) {
			LoggingUtils.ERROR("Failed to read lines from file: %s", exception.getMessage());
		}
		
		return logLines;
	}
}
