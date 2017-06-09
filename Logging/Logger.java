package Logging;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public final class Logger {
	private static File homeDirectory = null;
	private static File logFile = null;
	private static FileWriter fw = null;
	private static BufferedWriter bw = null;
	private static PrintWriter pw = null;
	private static boolean initialized = false;
	
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

	private static void initialize() throws IOException {
		homeDirectory = new File(System.getProperty("user.home")).getAbsoluteFile();
		logFile = homeDirectory.toPath().resolve("awed.log").toFile();
		fw = new FileWriter(logFile, true);
		bw = new BufferedWriter(fw);
		pw = new PrintWriter(bw);
	}
}
