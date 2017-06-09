package MusicPlayer;

import java.io.File;
import java.util.LinkedList;

import WeightedPlaylist.AudioSource;
import WeightedPlaylist.MusicFile;
import WeightedPlaylist.Playlist;
import javafx.scene.media.MediaPlayer;

class MusicPlayer {
	private static final int historySize = 100;
	private Playlist playlist = null;
	private MusicFile prevMF = null;
	private MusicFile currMF = null;
	private MusicFile nextMF = null;
	private LinkedList<MusicFile> upNext = null;
	private LinkedList<MusicFile> lastPlayed = null;
	private MediaWindow mw = null;

	protected MusicPlayer(File folder, MediaWindow mediaWindow) {
		mw = mediaWindow;
		playlist = AudioSource.getPrimarySource(folder);
		if (playlist.isValid()) {
			upNext = new LinkedList<MusicFile>();
			lastPlayed = new LinkedList<MusicFile>();
			prevMF = getValidSong();
			currMF = getValidSong();
			nextMF = getValidSong();
			play();
		}	
	}

	private MusicFile getValidSong() {
		MusicFile nextFile = playlist.getSong(); //test
		finalize(nextFile);
		while (!nextFile.isValid()) {
			nextFile = playlist.getSong();
			finalize(nextFile);
		}
		return nextFile;
	}

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

	protected double getRatio() {
		return currMF.player().getCurrentTime().toSeconds() / currMF.player().getTotalDuration().toSeconds();
	}

	protected void play() {
		currMF.player().play();
		if (currMF.player().getError() != null) {
			currMF = nextMF;
			nextMF = getNext();
			play();
		}
	}

	protected void pause() {
		if (currMF.player().getStatus() == MediaPlayer.Status.PAUSED
				|| currMF.player().getStatus() == MediaPlayer.Status.STOPPED
				|| currMF.player().getStatus() == MediaPlayer.Status.READY) {
			play();
		} else {
			currMF.player().pause();
		}
	}

	protected void skipNext() {
		currMF.modifyWeight(0.5);
		playNext();
	}

	protected void playNext() {
		currMF.player().stop();
		remember(prevMF);
		prevMF = currMF;
		currMF = nextMF;
		play();
		nextMF = getNext();
	}

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

	protected void favorite() {
		currMF.modifyWeight(2.0);
	}

	protected void dontPlay() {
		currMF.modifyWeight(0.0);
		playNext();
	}

	protected void seek(double ratio) {
		currMF.player().seek(currMF.player().getTotalDuration().multiply(ratio));
	}

	private void remember(MusicFile mf) {
		mf.dispose();
		lastPlayed.addFirst(mf);
		if (lastPlayed.size() > historySize) {
			lastPlayed.removeLast();
		}
	}

	private void queue(MusicFile mf) {
		mf.dispose();
		upNext.addFirst(mf);
	}

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

	private MusicFile getPrev() {
		if (lastPlayed.size() > 0) {
			MusicFile mf = lastPlayed.removeFirst();
			finalize(mf);
			return mf;
		} else {
			return null;
		}
	}

	private boolean hasPrev() {
		return (lastPlayed.size() > 0);
	}

	protected boolean isValid() {
		return playlist.isValid();
	}
}
