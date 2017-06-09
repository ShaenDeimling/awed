package WeightedPlaylist;

import java.io.File;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public final class MusicFile extends AudioSource {

	private boolean isValid = true;

	private double weight = 0.0;

	private MediaPlayer mediaPlayer = null;
	private Media media = null;

	protected MusicFile(File source) {
		super(source);
		if (Double.isNaN(weight)) {
			isValid = false;
		}
	}

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

	public MediaPlayer player() {
		if (isValid) {
			return mediaPlayer;
		} else {
			return null;
		}
	}

	public void dispose() {
		mediaPlayer.stop();
		mediaPlayer.dispose();
		mediaPlayer = null;
		media = null;
	}

	private void invalidate() {
		isValid = false;
		weight = Double.NaN;
	}

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

	/**
	 * Multiply the current weight by a value
	 */
	public void modifyWeight(double modifier) {
		balanceWeight(modifier);
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
