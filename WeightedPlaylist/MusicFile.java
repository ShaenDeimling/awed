package WeightedPlaylist;

import java.io.File;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Represents a file that contains playable audio.
 */
public final class MusicFile extends AudioSource {
	
	/**
	 * True iff this file can actually be played.
	 */
	private boolean isValid = true;

	/**
	 * The relative likelihood of this file being played. 1.0 is average.
	 */
	private double weight = 0.0;

	/**
	 * The media player used to play this file.
	 */
	private MediaPlayer mediaPlayer = null;
	
	/**
	 * The media created using this file.
	 */
	private Media media = null;

	/**
	 * Acquire the resources needed to play this song immediately.
	 */
	public void acquire() {
		if (!isValid) {
			return;
		}
		
		if (mediaPlayer != null) {
			dispose();
		}
		
		try {
			media = new Media(getFile().toURI().toASCIIString());
			mediaPlayer = new MediaPlayer(media);
			if (media.getError() != null || mediaPlayer.getError() != null) {
				throw new Exception();
			}
		} catch (Exception ex) {
			if (mediaPlayer != null) {
				mediaPlayer.dispose();
				mediaPlayer = null;
			}
			media = null;
			invalidate();
		}
	}
	
	/**
	 * Returns the playable media player.
	 */
	public MediaPlayer player() {
		if (isValid) {
			return mediaPlayer;
		} else {
			return null;
		}
	}

	/**
	 * Disposes of resources allocated to this object.
	 */
	public void dispose() {
		mediaPlayer.stop();
		mediaPlayer.dispose();
		mediaPlayer = null;
		media = null;
	}

	/**
	 * Multiply the current weight by a value.
	 */
	public void modifyWeight(double modifier) {
		balanceWeight(modifier);
	}
	
	/**
	 * Used to ensure this file is never played again.
	 */
	private void invalidate() {
		isValid = false;
		weight = Double.NaN;
	}
	
	/**
	 * Creates a new MusicFile from the specified file.
	 * 
	 * @param source The file to attempt to play.
	 */
	protected MusicFile(File source) {
		super(source);
		if (Double.isNaN(weight)) {
			isValid = false;
		}
	}

	/**
	 * True iff this file has not been invalidated.
	 */
	@Override
	public boolean isValid() {
		return isValid;
	}

	@Override
	protected void balanceWeight(double multiplier) {
		if (isValid) {
			setWeight(getWeight() * multiplier);
		}
	}

	@Override
	protected int getCount() {
		if (isValid) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	protected double getWeight() {
		if (isValid) {
			return weight;
		} else {
			return 0.0;
		}
	}

	@Override
	protected void setWeight(double total) {
		weight = total;
		if (Double.isNaN(weight)) {
			isValid = false;
		}
	}

	@Override
	protected MusicFile getSong(double residual) {
		if (isValid) {
			return this;
		}
		return null;
	}

	@Override
	protected double updateCumulative() {
		return getWeight();
	}

	@Override
	protected String getFileString() {
		return Double.toString(weight) + " " + getName();
	}

}
