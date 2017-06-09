package WeightedPlaylist;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;

import Logging.Logger;

//represents a folder with songs and has a file in that folder

public final class Playlist extends AudioSource {
	private File preferences = null;
	private ArrayList<AudioSource> audioSources = null;
	private boolean isValid = false;
	private double totalWeight = Double.NaN;
	private MusicFile onePlayed = null;
	private MusicFile twoPlayed = null;

	protected Playlist(File dir) {
		super(dir);
		HashMap<String, AudioSource> sourceMap = new HashMap<String, AudioSource>();
		try {
			if (!getFile().isDirectory()) {
				throw new Exception("A folder containing songs must be selected to play.");
			}
			preferences = getFile().toPath().resolve("awed.txt").toFile();

			// to make sure all sources are enumerated
			if (!preferences.exists()) {
				preferences.createNewFile();
			} else {
				// read in everything that has been seen
				Scanner fileReader = new Scanner(preferences);
				while (fileReader.hasNextLine()) {
					String line = fileReader.nextLine();
					String parts[] = line.split(" ", 2);
					if (parts.length != 2) {
						continue;
					}
					File path = getFile().toPath().resolve(parts[1]).toFile();
					double childWeight = 1.0;
					try {
						childWeight = Double.parseDouble(parts[0]);
					} catch (Exception e) {
						childWeight = 1.0;
					}
					if (Double.isInfinite(childWeight) || childWeight < 0.0) {
						childWeight = 1.0;
					}
					AudioSource source = AudioSource.getSource(path, childWeight);
					sourceMap.put(source.getName(), source);
				}
				fileReader.close();

			}
		} catch (Exception ex) {
			Logger.log(ex.toString());
		}

		try {
			audioSources = new ArrayList<AudioSource>();

			// get all the audio sources
			for (File child : getFile().listFiles()) {
				AudioSource source = sourceMap.get(child.getName());
				// if it is new
				if (source == null) {
					audioSources.add(AudioSource.getSource(child, 1.0));
				} else {
					audioSources.add(source);
				}
			}

			isValid = true;
		} catch (Exception ex) {
			isValid = false;
			Logger.log(ex.toString());
		}
		
		if (getCount() < 3) {
			isValid = false;
		}
	}

	private void updateFile() {
		try (FileWriter fw = new FileWriter(preferences, false);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw = new PrintWriter(bw);) {

			Collections.sort(audioSources, new Comparator<AudioSource>() {

				@Override
				public int compare(AudioSource o1, AudioSource o2) {
					if (o1 instanceof Playlist) {
						if (o2 instanceof Playlist) {
							if (((Playlist) o1).getNormWeight() > ((Playlist) o2).getNormWeight()) {
								return -1;
							} else {
								return 1;
							}
						} else if (o2 instanceof MusicFile) {
							return -1;
						}
					} else if (o1 instanceof MusicFile) {
						if (o2 instanceof Playlist) {
							return 1;
						} else if (o2 instanceof MusicFile) {
							if (((MusicFile) o1).getWeight() > ((MusicFile) o2).getWeight()) {
								return -1;
							} else {
								return 1;
							}
						}
					}

					return 0;
				}
			});

			boolean lastFile = true;
			for (int i = 0; i < audioSources.size(); i++) {
				AudioSource as = audioSources.get(i);
				if (as instanceof Playlist) {
					((Playlist) as).updateFile();
				} else if (as instanceof MusicFile) {
					if (lastFile) {
						lastFile = false;
						if (i != 0) {
							pw.println();
						}
					}
				}
				pw.println(as.getFileString());
			}

			pw.flush();
			pw.close();
		} catch (Exception ex) {
			Logger.log(ex.toString());
		}

	}

	public MusicFile getSong() {
		MusicFile next = getSong(AudioSource.random());
		while (next.equals(onePlayed) || next.equals(twoPlayed)) {
			next = getSong(AudioSource.random());
		}
		onePlayed = twoPlayed;
		twoPlayed = next;
		return next;
	}

	@Override
	protected MusicFile getSong(double residual) {
		if (!Double.isNaN(totalWeight)) {
			updateWeights();
		}
		
		int index = 0;
		for (int i = 0; i < audioSources.size(); i++) {
			if (residual <= audioSources.get(i).getCumulWeight()) {
				index = i;
				break;
			}
		}

		AudioSource as = audioSources.get(index);
		int loopCount = 0;
		while (!as.isValid() && loopCount < audioSources.size()) {
			index = (index + 1) % audioSources.size();
			as = audioSources.get(index);
			loopCount++;
		}

		if (index == 0) {
			residual = residual / as.getCumulWeight();
		} else {
			double minimum = audioSources.get(index - 1).getCumulWeight();
			residual = (residual - minimum) / (as.getCumulWeight() - minimum);
		}

		return as.getSong(residual);
	}

	@Override
	protected void balanceWeight(double multiplier) {
		for (AudioSource as : audioSources) {
			as.balanceWeight(multiplier);
		}
	}

	@Override
	protected int getCount() {
		int count = 0;
		for (AudioSource as : audioSources) {
			count += as.getCount();
		}
		return count;
	}

	@Override
	protected double getWeight() {
		double totalWeight = 0.0;
		for (AudioSource as : audioSources) {
			totalWeight += as.getWeight();
		}
		return totalWeight;
	}

	@Override
	protected void setWeight(double total) {
		double multiplier = total / getWeight();
		balanceWeight(multiplier);
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	/**
	 * Used only on the top playlist.
	 */
	protected void updateWeights() {
		if (Double.isNaN(totalWeight)) {
			try {
				File parent = getFile().toPath().resolve("../awed.txt").normalize().toAbsolutePath().toFile();
				if (parent.exists()) {
					Scanner fileReader = new Scanner(parent);
					while (fileReader.hasNextLine()) {
						String line = fileReader.nextLine();
						String parts[] = line.split(" ", 2);
						if (parts.length != 2) {
							continue;
						}
						File path = getFile().getParentFile().toPath().resolve(parts[1]).toFile();
						if (path.getAbsoluteFile().equals(getFile())) {
							double childWeight = 1.0;
							try {
								childWeight = Double.parseDouble(parts[0]);
							} catch (Exception e) {
								childWeight = 1.0;
							}
							totalWeight = childWeight;
						}
					}
					fileReader.close();
				}
			} catch (Exception ex) {
				Logger.log(ex.toString());
			}
			if (Double.isInfinite(totalWeight) || totalWeight < 0.0 || Double.isNaN(totalWeight)) {
				totalWeight = 1.0;
			}
		}
		setWeight(totalWeight * getCount());
		updateFile();
		updateCumulative();
	}

	@Override
	protected double updateCumulative() {
		double cumulativeWeight = 0;
		for (int i = 0; i < audioSources.size(); i++) {
			AudioSource as = audioSources.get(i);
			double temp = as.updateCumulative();
			if (!Double.isNaN(temp)) {
				cumulativeWeight += temp;
			}
			as.setCumulWeight(cumulativeWeight);
		}
		if (cumulativeWeight <= 0.0) {
			cumulativeWeight = 1.0;
		}
		for (int i = 0; i < audioSources.size() - 1; i++) {
			AudioSource as = audioSources.get(i);
			as.setCumulWeight(as.getCumulWeight() / cumulativeWeight);
		}
		audioSources.get(audioSources.size() - 1).setCumulWeight(1.0);

		return cumulativeWeight;
	}

	@Override
	protected String getFileString() {
		return Double.toString(getNormWeight()) + " " + getName();
	}

	private double getNormWeight() {
		return getWeight() / (double) getCount();
	}

}
