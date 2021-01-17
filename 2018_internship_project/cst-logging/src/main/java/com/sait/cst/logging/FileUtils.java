package com.sait.cst.logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

public class FileUtils {
	/**
	 * Creates a directory specified by pathname argument if it doesn't exist already. If directory
	 * exists, then File instance will be returned. If creating directory fails for some reason, null
	 * will be returned.
	 */
	public static boolean createDirectory(String pathname) {
		File newDirectory = new File(pathname);

		if (newDirectory.exists()) {
			return true;
		}

		return newDirectory.mkdirs();
	}

	/**
	 * Creates a BufferedWriter instance that can be used to write content to the file specified by
	 * directoryPath and filename. If file provided already exists, it will throw an exception as it
	 * is not desirable to overwrite existing log files.
	 */
	public static BufferedWriter createBufferedWriter(String directoryPath, String filename) throws IOException {
		File file = new File(directoryPath, filename);

		// check if file exists, and throw an exception if so.
		if (file.exists()) {
			throw new FileAlreadyExistsException(file.getAbsolutePath());
		}

		return new BufferedWriter(new FileWriter(file));
	}

	public static BufferedReader createBufferedReader(File file) throws FileNotFoundException {
		return new BufferedReader(new FileReader(file));
	}
}
