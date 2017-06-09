package MusicPlayer;

import java.io.File;
import java.util.LinkedList;

import WeightedPlaylist.AudioSource;
import WeightedPlaylist.MusicFile;
import WeightedPlaylist.Playlist;

import javafx.scene.media.MediaPlayer;

/**
 * This class is used to abstract the song selection.
 */
class MusicPlayer {
	/**
	 * The maximum number of songs to remember.
	 */
	private static final int historySize = 100;
	
	/**
	 * The playlist from which songs are selected.
	 */
	private Playlist playlist = null;
	
	/**
	 * The last played music file.
	 */
	private MusicFile prevMF = null;
	
	/**
	 * The current music file.
	 */
	private MusicFile currMF = null;
	
	/**
	 * The next music file to be played.
	 */
	private MusicFile nextMF = null;
	
	/**
	 * The list of songs that will be played after {@link #nextMF}.
	 */
	private LinkedList<MusicFile> upNext = null;
	
	/**
	 * The list of songs played before {@link #prevMF}.
	 */
	private LinkedList<MusicFile> lastPlayed = null;
	
	/**
	 * The media window which is using this media player.
	 */
	private MediaWindow mw = null;

	/**
	 * Creates a new music player from the give folder.
	 * 
	 * @param folder A folder containing media files.
	 * @param mediaWindow The media window where the songs will be displayed.
	 */
	protected MusicPlayer(File folder, MediaWindow mediaWindow) {
		mw = mediaWindow;
		playlist = AudioSource.getPrimarySource(folder);
		upNext = new LinkedList<MusicFile>();
		lastPlayed = new LinkedList<MusicFile>();
		prevMF = getValidSong();
		currMF = getValidSong();
		nextMF = getValidSong();
		if (playlist.isValid()) {
			play();
		}	
	}

	/**
	 * Gets a media file which can be played.
	 */
	private MusicFile getValidSong() {
		if (!playlist.isValid()) {
			return null;
		}
		MusicFile nextFile = null;
		do {
			nextFile = playlist.getSong();
			if (nextFile != null) {
				finalize(nextFile);
			}
		} while (nextFile == null || !nextFile.isValid());
		return nextFile;
	}

	/**
	 * Acquires resources for playing the music file.
	 * 
	 * @param mf The music file to be finalized.
	 */
	private void finalize(MusicFile mf) {
		mf.acquire();
		mw.initMediaPlayer(mf.player());
		mf.player().setOnPlaying(new Runnable() {
			@Override
			public void run() {
				int index = mf.getName().lastIndexOf(".");
				String title = mf.getName();
				if (index != -1) {
					title = title.substring(0, index);
				}
				mw.setTitle(title);
			}
		});
	}
	
	/**
	 * Determine the current place in the song.
	 * 
	 * @return The ratio from 0.0 to 1.0 of song completion.
	 */
	protected double getRatio() {
		return currMF.player().getCurrentTime().toSeconds() / currMF.player().getTotalDuration().toSeconds();
	}

	/**
	 * Plays the current song.
	 */
	protected void play() {
		currMF.player().play();
		if (currMF.player().getError() != null) {
			currMF = nextMF;
			nextMF = getNext();
			play();
		}
	}

	/**
	 * Pauses the current song.
	 */
	protected void pause() {
		if (currMF.player().getStatus() == MediaPlayer.Status.PAUSED
				|| currMF.player().getStatus() == MediaPlayer.Status.STOPPED
				|| currMF.player().getStatus() == MediaPlayer.Status.READY) {
			play();
		} else {
			currMF.player().pause();
		}
	}

	/**
	 * Skips to the next song. Reduces likelihood of playing this song again.
	 */
	protected void skipNext() {
		currMF.modifyWeight(0.5);
		playNext();
	}

	/**
	 * Plays the next song. No impact on the likelihood of playing this song again.
	 */
	protected void playNext() {
		currMF.player().stop();
		remember(prevMF);
		prevMF = currMF;
		currMF = nextMF;
		play();
		nextMF = getNext();
	}

	/**
	 * Play the previously played song.
	 */
	protected void previous() {
		if (hasPrev()) {
			currMF.player().stop();
			queue(nextMF);
			nextMF = currMF;
			currMF = prevMF;
			play();
			prevMF = getPrev();
		}
	}

	/**
	 * Increase the likelihood of this song being played again.
	 */
	protected void favorite() {
		currMF.modifyWeight(2.0);
	}

	/**
	 * Never play this song again. Go to the next song.
	 */
	protected void dontPlay() {
		currMF.modifyWeight(0.0);
		playNext();
	}

	/**
	 * Skip to a specified point in this song.
	 * 
	 * @param ratio A value from 0.0 (beginning) to 1.0 (end).
	 */
	protected void seek(double ratio) {
		currMF.player().seek(currMF.player().getTotalDuration().multiply(ratio));
	}

	/**
	 * Adds this song to the list of previously played songs. Free's resources.
	 * 
	 * @param mf The song to remember.
	 */
	private void remember(MusicFile mf) {
		mf.dispose();
		lastPlayed.addFirst(mf);
		if (lastPlayed.size() > historySize) {
			lastPlayed.removeLast();
		}
	}
	
	/**
	 * Queues a song to be played later. Used when navigating to previous songs.
	 * 
	 * @param mf The music file to be queued.
	 */
	private void queue(MusicFile mf) {
		mf.dispose();
		upNext.addFirst(mf);
	}

	/**
	 * Gets the next queued song. If there are none, obtains a random, valid music file.
	 * 
	 * @return A music file which can be played.
	 */
	private MusicFile getNext() {
		MusicFile mf = null;
		if (upNext.size() > 0) {
			mf = upNext.removeFirst();
			finalize(mf);
		} else {
			mf = getValidSong();
		}
		return mf;
	}
	
	/**
	 * Gets the last song in the history. Returns null when the list is depleted.
	 * 
	 * @return A music file which has been played.
	 */
	private MusicFile getPrev() {
		if (lastPlayed.size() > 0) {
			MusicFile mf = lastPlayed.removeFirst();
			finalize(mf);
			return mf;
		} else {
			return null;
		}
	}

	/**
	 * True iff there are more songs in the history.
	 */
	private boolean hasPrev() {
		return (lastPlayed.size() > 0);
	}

	/**
	 * True iff there are at least three valid music files in the folder.
	 */
	protected boolean isValid() {
		return playlist.isValid();
	}
}
