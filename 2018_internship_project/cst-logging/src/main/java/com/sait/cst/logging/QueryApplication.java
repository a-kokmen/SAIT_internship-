package com.sait.cst.logging;

import java.io.File;
import java.io.FilenameFilter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_FixedWidth;
import de.vandermeer.asciithemes.a7.A7_Grids;

public class QueryApplication {
	public static void run(String logDirectory, String type, Instant start, Instant end) {
		// gets a list of all log files (matching the extension) under the directory provided.
		File directory = new File(logDirectory);
		File[] files = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith("." + Application.LOG_FILENAME_EXTENSION);
			}
		});

		// if there are no files found in the directory, prints an error message and exits.
		if (files == null) {
			LoggingUtils.ERROR("Failed to list files in directory (%s).", logDirectory);
			System.exit(-2);
		}

		// if start and end aren't provided, then use meaningful default values which will effectively
		// allow us to not filter out anything because no timestamp in the file will be smaller than 0
		// or larger than Long.MAX_VALUE.
		long startTimestamp = start == null ? 0 : start.toEpochMilli();
		long endTimestamp = end == null ? Long.MAX_VALUE : end.toEpochMilli();

		LoggingUtils.DEBUG("Detected %d files in log directory provided (%s).", files.length, logDirectory);
		for (File file : files) { 
			LoggingUtils.DEBUG(">>>>> %s", file.getName());

			// read the file using LogReader and get matching lines.
			LogReader logReader = new LogReader(file);
			List<LogLine> logLines = logReader.getMatchingLogLines(type, startTimestamp, endTimestamp);
			
			// if table isn't empty, print a table.
			if (logLines.isEmpty()) {
				LoggingUtils.DEBUG("No matching log lines were found.");
			} else {
				printTable(file, logLines);
			}

			LoggingUtils.DEBUG("<<<<< %s", file.getName());
		}
	}
	
	private static void printTable(File file, List<LogLine> logLines) {
		// use AsciiTable library to generate a nice looking table with all the column names.
		AsciiTable table = new AsciiTable();
		table.addRule();
		
		// cwc (column width calculator) is used to determine widths per column in the table. we will
		// set each column width depending on the value returned for matches log lines.
		CWC_FixedWidth cwc = new CWC_FixedWidth();

		// iterate over all the lines and get their fields to add to the table. in the first iteration
		// we add the field names as column headers.
		//
		// fields is a map of field name -> field value. e.g. DFS State -> 3
		for (int i = 0; i < logLines.size(); i++) {
			Map<String, String> fields = logLines.get(i).getFields();

			// first row in the table is for headers (addRule() adds the line).
			if (i == 0) {
				table.addRow(fields.keySet());
				table.addRule();
				
				// collect column lengths for the column width calculator.
				for (String column : fields.keySet()) {
					cwc.add(column.length());
				}
			}
			
			// add the actual values of the row.
			table.addRow(fields.values());
		}

		// finish up the table by adding a bottom line. then print it following the file name
		// to users can understand where values are coming from.
		table.addRule();
		System.out.format("File: %s%n", file.getName());

		// set column width calculator before rendering the table.
		table.getRenderer().setCWC(cwc);
		table.getContext().setGrid(A7_Grids.minusBarPlusEquals());

		System.out.println(table.render());
	}
}
