package WeightedPlaylist;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

/**
 * Abstract audio source representation.
 */
public abstract class AudioSource {
	/**
	 * Used to generate random numbers.
	 */
	private static final Random random = new Random();

	/**
	 * A list of folders to be rebalanced once initialized.
	 */
	private static ArrayList<Playlist> toBalance = null;

	/**
	 * Used during song selection.
	 */
	private double cumulWeight = Double.NaN;

	/**
	 * This file or folder this audio source represents.
	 */
	private File file = null;

	/**
	 * The name of this audio source's file or folder.
	 */
	private String name = null;

	/**
	 * The total count of valid songs in this audio source.
	 */
	protected abstract int getCount();

	/**
	 * The total weight of all valid songs in this audio source.
	 */
	protected abstract double getWeight();

	/**
	 * Multiply this audio source and all its children's weight by a multiplier.
	 * 
	 * @param multiplier
	 *            The weight multiplier.
	 */
	protected abstract void balanceWeight(double multiplier);

	/**
	 * Ensure the weight of this audio source is equal to total.
	 * 
	 * @param total
	 *            The new total weight.
	 */
	protected abstract void setWeight(double total);

	/**
	 * True iff this audio source can return a valid {@link MusicFile}.
	 */
	protected abstract boolean isValid();

	/**
	 * Returns a valid music file based on the double passed in.
	 * 
	 * @param residual
	 *            The amount over this song's cumulative weight.
	 * @return A valid music file.
	 */
	protected abstract MusicFile getSong(double residual);

	/**
	 * Forces an update of the cumulative weights of all descendant audio
	 * sources.
	 * 
	 * @return The total cumulative weight of this audio source.
	 */
	protected abstract double updateCumulative();

	/**
	 * The string that should be stored in a file to retain this audio source's
	 * chance of being played.
	 */
	protected abstract String getFileString();

	/**
	 * Gets a playlist that will act as a primary audio source.
	 * 
	 * @param topDir
	 *            The playlist directory.
	 * @return A playlist object with songs.
	 */
	public static Playlist getPrimarySource(File topDir) {
		if (!topDir.isDirectory()) {
			return null;
		}
		toBalance = new ArrayList<Playlist>();
		Playlist temp = new Playlist(topDir);
		if (temp.isValid()) {
			temp.setWeight(temp.getCount());
			for (Playlist pl : toBalance) {
				pl.setWeight(pl.getCumulWeight());
			}
			temp.updateWeights();
		}
		toBalance.clear();
		toBalance = null;
		return temp;
	}

	/**
	 * The string representing the file or folder of this audio source.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * The file object representing the source for this object.=
	 */
	protected final File getFile() {
		return file;
	}

	/**
	 * The constructor used by inheriting objects.
	 */
	protected AudioSource(File source) {
		file = source.getAbsoluteFile();
		name = file.getName();
	}

	/**
	 * Used to set the cumulative weight of an audio source. Should be between 0
	 * and 1.
	 */
	protected final void setCumulWeight(double weight) {
		cumulWeight = weight;
	}

	/**
	 * Returns this object's cumulative weight.
	 */
	protected final double getCumulWeight() {
		return cumulWeight;
	}

	/**
	 * Loads a particular file into an audio source object. It is given the
	 * specified initial weight.
	 * 
	 * @param source
	 *            The file or folder source.
	 * @param weight
	 *            The initial weight of the audio source.
	 * @return An audio source object.
	 */
	protected static AudioSource getSource(File source, double weight) {
		if (source.isDirectory()) {
			Playlist playlist = new Playlist(source);
			playlist.setCumulWeight(playlist.getCount() * weight);
			toBalance.add(playlist);
			return playlist;
		} else {
			MusicFile musicFile = new MusicFile(source);
			musicFile.setWeight(weight);
			return musicFile;
		}
	}

	/**
	 * Returns a random double between 0 and 1.
	 * @return
	 */
	protected static double random() {
		return random.nextDouble();
	}
}
