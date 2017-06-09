package WeightedPlaylist;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public abstract class AudioSource {
	private static final Random random = new Random();
	private static Playlist top = null;
	private static ArrayList<Playlist> toBalance = null;
	private double cumulWeight = Double.NaN;
	private File file = null;
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

	protected abstract boolean isValid();
	protected abstract MusicFile getSong(double residual);
	protected abstract double updateCumulative();
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
		top = new Playlist(topDir);
		if (top.isValid()) {
			top.setWeight(top.getCount());
			for (Playlist pl : toBalance) {
				pl.setWeight(pl.getCumulWeight());
			}
			toBalance.clear();
			toBalance = null;
			top.updateWeights();
		}
		Playlist temp = top;
		top = null;
		return temp;
	}
	
	protected static void rebalanceAll(double multplier) {
		top.balanceWeight(multplier);
	}

	public final String getName() {
		return name;
	}

	protected final File getFile() {
		return file;
	}

	protected AudioSource(File source) {
		file = source.getAbsoluteFile();
		name = file.getName();
	}

	protected final void setCumulWeight(double weight) {
		cumulWeight = weight;
	}

	protected final double getCumulWeight() {
		return cumulWeight;
	}

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

	protected static double random() {
		return random.nextDouble();
	}
}
