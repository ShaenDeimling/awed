package Logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Used to handle logging errors.
 */
public final class Logger {
	/**
	 * The user's home directory.
	 */
	private static File homeDirectory = null;
	
	/**
	 * The file to log to.
	 */
	private static File logFile = null;
	
	/**
	 * The log file's file writer.
	 */
	private static FileWriter fw = null;
	
	/**
	 * Buffered wrapped for the file writer.
	 */
	private static BufferedWriter bw = null;
	
	/**
	 * Print wrapper for the buffered writer.
	 */
	private static PrintWriter pw = null;
	
	/**
	 * True iff the file has been initialized for writing.
	 */
	private static boolean initialized = false;
	
	/**
	 * Logs a message to the log file. If this fails, prints to standard out.
	 * @param message The message to be logged.
	 */
	public static void log(String message) {
		if (!initialized) {
			try {
				initialize();
				initialized = true;
			} catch (Exception ex) {
				initialized = false;
				System.out.println("Failed to open the log file for this reason:");
				System.out.println(ex.toString());
			}
		}
		
		try {
			pw.println(message);
			pw.flush();
		} catch (Exception ex) {
			System.out.println("Error writing the follwoing message to the log file:");
			System.out.println(message);
			System.out.println("Failed to write to the log file for this reason:");
			System.out.println(ex.toString());
		}
	}

	/**
	 * Tries to obtain the file for writing.
	 */
	private static void initialize() throws IOException {
		homeDirectory = new File(System.getProperty("user.home")).getAbsoluteFile();
		logFile = homeDirectory.toPath().resolve("awed.log").toFile();
		fw = new FileWriter(logFile, true);
		bw = new BufferedWriter(fw);
		pw = new PrintWriter(bw);
	}
}
