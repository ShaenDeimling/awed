package MusicPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.prefs.Preferences;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

// ignore music files longer than 10 min?
// songs start at 10, fade to 1?
// in file have rating followed by name
// have folder names with modifier
// have folders first and a blank line between
// use NaN for files that are not songs
// ex:
// 1.5 file.mp3
// 1.2 dir

/**
 * Plays audio files and provides other useful functionality including an audio
 * visualizer.
 * 
 * @author Shaen Deimling
 *
 */
public class MediaWindow extends Application {

	/**
	 * Used to store user preferences.
	 */
	private static final Preferences preferences = Preferences.userNodeForPackage(MediaWindow.class);

	/**
	 * The music player that will provide abstraction for song selection.
	 */
	private MusicPlayer musicPlayer = null;

	/**
	 * The folder that was previously opened by the user.
	 */
	private File lastPickedFolder = null;

	/**
	 * The list of buttons; used to allow for faster processing.
	 */
	private ArrayList<FadeButton> buttons = null;

	/**
	 * The graphics context of the canvas object.
	 */
	private GraphicsContext graphics = null;

	/**
	 * The canvas onto which all music visuals are drawn.
	 */
	private Canvas canvas = null;

	/**
	 * The spectrum listener which updates the music visualizer.
	 */
	private AudioSpectrumListener visual = null;

	/**
	 * The folder chooser which allows the user to select the media location.
	 */
	private DirectoryChooser folderChooser = null;

	/**
	 * The stage in which the components of the application are placed.
	 */
	private Stage stage = null;

	/**
	 * The blur that is applied to the canvas after drawing.
	 */
	private BoxBlur blur = null;

	/**
	 * The random number generator used for song selection among other things.
	 */
	private Random rand = null;

	/**
	 * The stack pane that contains the various content panes of the
	 * application.
	 */
	private StackPane mainPane = null;

	/**
	 * The list of panes which are placed into the main stack pane.
	 */
	private LinkedList<Pane> innerPane = null;

	/**
	 * The object representing the orbs in the music visualizer.
	 */
	private BassBall[] dots = null;

	/**
	 * The timer used to update the visuals of the music player.
	 */
	private AnimationTimer timer = null;

	/**
	 * The x values where each bin of the audio spectrum is drawn.
	 */
	private int[] xs = null;

	/**
	 * The widths of the areas in which each bin of the audio spectrum is drawn.
	 */
	private int[] widths = null;

	/**
	 * The hues applied to each bin of the audio visualizer.
	 */
	private double[] hues = null;

	/**
	 * The brightness of each bin of the audio visualizer.
	 */
	private double[] brightness = null;

	/**
	 * The array of audio amplitudes updated by the spectrum listener.
	 */
	private double[] newAmplitudes = null;

	/**
	 * The shift in the hues due to the duration remaining in the song.
	 */
	private double colorShift = 0.0;

	/**
	 * The number of orbs drawn on the screen.
	 */
	private int speckCount = 0; // 256

	/**
	 * The number of bin into which the audio spectrum is divided.
	 */
	private int bandCount = 1024;

	/**
	 * Play the next song.
	 */
	private final Runnable playNext = new Runnable() {
		@Override
		public void run() {
			if (musicPlayer != null) {
				musicPlayer.playNext();
			}
		}
	};

	/**
	 * Starts the application.
	 * 
	 * @param args
	 *            The command line arguments.
	 */
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage s) throws Exception {
		stage = s;
		stage.setTitle("Awed");
		stage.setMinHeight(200);
		stage.setMinWidth(300);
		stage.setHeight(400);
		stage.setWidth(800);

		rand = new Random();

		innerPane = new LinkedList<Pane>();
		dots = new BassBall[speckCount];
		for (int i = 0; i < dots.length; i++) {
			dots[i] = new BassBall(rand.nextDouble() * 360.0);
		}

		xs = new int[bandCount];
		widths = new int[bandCount];
		hues = new double[bandCount];
		brightness = new double[bandCount];

		folderChooser = new DirectoryChooser();
		folderChooser.setTitle("Playlist Folder Selection");

		canvas = new Canvas();
		canvas.widthProperty().addListener(e -> redraw());
		canvas.heightProperty().addListener(e -> redraw());

		graphics = canvas.getGraphicsContext2D();

		FadeButton.setMaxOpacity(0.5);

		FadeButton playButton = new FadeButton("Play/Pause");
		FadeButton nextButton = new FadeButton("Next Song");
		FadeButton prevButton = new FadeButton("Previous Song");
		FadeButton clickSeek = new FadeButton("Seek", Color.LIGHTGRAY);
		FadeButton openFolder = new FadeButton("Open Playlist", Color.WHITE);
		FadeButton neverAgain = new FadeButton("Never Again", Color.RED);
		FadeButton favorite = new FadeButton("Favorite", Color.GREEN);
		FadeButton fullScreen = new FadeButton("Toggle Fullscreen", Color.WHITE);

		buttons = new ArrayList<FadeButton>();
		buttons.add(playButton);
		buttons.add(nextButton);
		buttons.add(prevButton);
		buttons.add(openFolder);
		buttons.add(clickSeek);
		buttons.add(fullScreen);
		buttons.add(neverAgain);
		buttons.add(favorite);

		blur = new BoxBlur();
		blur.setHeight(4.0);
		blur.setWidth(4.0);
		blur.setIterations(2);

		// used to create the visuals; lots of custom math
		visual = new AudioSpectrumListener() {

			@Override
			public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
				double[] amplitudes = new double[magnitudes.length];
				for (int i = 0; i < magnitudes.length - 1; i++) {
					double interpolate = (double) i / (double) (magnitudes.length);
					double scale = 50;
					double stretch = 0.5;
					interpolate = stretch * (Math.pow(scale, interpolate) - 1.0) / (scale - 1.0);
					interpolate *= (double) (magnitudes.length);
					int index = (int) interpolate;
					double ratio = interpolate - (double) (index);

					double result1 = Double.min(1.0, Math.sqrt(Math.abs((magnitudes[index] + 60.0) / 50.0)));
					double result2 = Double.min(1.0, Math.sqrt(Math.abs((magnitudes[index + 1] + 60.0) / 50.0)));
					double result = result1 * (1.0 - ratio) + result2 * ratio;
					amplitudes[i] = result;
				}

				newAmplitudes = amplitudes;
			}
		};

		// used to animate the moving orbs; more custom math
		timer = new AnimationTimer() {

			@Override
			public void handle(long now) {
				double[] amplitudes = newAmplitudes;
				// newAmplitudes = null;
				if (amplitudes != null) {
					graphics.setFill(Color.BLACK);
					graphics.fillRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());

					double ratio = 0.0;
					if (musicPlayer != null) {
						ratio = musicPlayer.getRatio();
					}

					colorShift = 360.0 * (1.0 - ratio);

					double bassAmplitude = 0.0;
					double maxAmplitude = 0.0;

					for (int i = 0; i < bandCount; i++) {
						bassAmplitude = Double.max(bassAmplitude,
								amplitudes[i] * Math.pow((double) (bandCount - i) / (double) bandCount, 10.0));
						maxAmplitude = Double.max(maxAmplitude, amplitudes[i]);

						brightness[i] = brightness[i] * 0.9 + 0.1
								* Math.pow(0.5 * (Math.pow(brightness[i], 2.0) + Math.pow(amplitudes[i], 2.0)), 0.5);

						graphics.setFill(Color.hsb(hues[i] + colorShift, 1.0, brightness[i]));
						graphics.fillRect(xs[i], 0, widths[i], canvas.getHeight());
					}

					maxAmplitude = Double.min(maxAmplitude, 1.0);
					BassBall.opacity = (0.8 * BassBall.opacity + 0.2 * Math.pow(maxAmplitude, 2.0)) * 0.9 + 0.1;
					Color ballColor = new Color(1.0, 1.0, 1.0, BassBall.opacity * 0.25);

					double screenWidth = (canvas.getWidth() + BassBall.diameter) / canvas.getWidth();
					double screenHeight = (canvas.getHeight() + BassBall.diameter) / canvas.getHeight();
					double speed = 0.002;
					for (BassBall dot : dots) {
						dot.direction += ((rand.nextDouble() - 0.5) * bassAmplitude * 5) * (1.0 - dot.x)
								+ 2.0 * Math.PI;
						dot.direction %= 2.0 * Math.PI;
						dot.x += Math.cos(dot.direction) * speed + screenWidth;
						dot.x %= screenWidth;
						dot.y += Math.sin(dot.direction) * speed + screenHeight;
						dot.y %= screenHeight;
						graphics.setFill(ballColor);
						graphics.fillOval(dot.x * canvas.getWidth() - BassBall.diameter,
								dot.y * canvas.getHeight() - BassBall.diameter, BassBall.radius, BassBall.radius);
					}

					graphics.applyEffect(blur);
				}

				for (FadeButton fb : buttons) {
					fb.setFill(new Color(fb.color.getRed(), fb.color.getGreen(), fb.color.getBlue(), fb.opacity));
					fb.opacity = Math.pow(fb.opacity, 1.02);
				}
			}
		};

		// stop the animations when the screen is minimized
		stage.iconifiedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue != null) {
					if (newValue.booleanValue()) {
						timer.stop();
					} else {
						timer.start();
					}
				}
			}
		});

		// pre-calculate the hues
		for (int i = 0; i < bandCount; i++) {
			hues[i] = (360.0 * ((double) i)) / ((double) bandCount);
		}
		redraw();

		mainPane = new StackPane();

		// add a fullscreen button
		fullScreen.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent ev) {
				fullScreen.opacity = FadeButton.maxOpacity;
				if (stage.isFullScreen()) {
					stage.setFullScreen(false);
				} else {
					stage.setFullScreen(true);
				}
			}
		});

		// add a button to never play this song again
		neverAgain.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent ev) {
				neverAgain.opacity = FadeButton.maxOpacity;
				if (musicPlayer != null) {
					musicPlayer.dontPlay();
				}
			}
		});

		// add a button to go back to the previous song
		prevButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent ev) {
				prevButton.opacity = FadeButton.maxOpacity;
				if (musicPlayer != null) {
					musicPlayer.previous();
				}
			}
		});

		// add a button to play or pause the song
		playButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent ev) {
				playButton.opacity = FadeButton.maxOpacity;
				if (musicPlayer != null) {
					musicPlayer.pause();
				}
			}
		});

		// add a button to skip songs; less likely to play again
		nextButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent ev) {
				nextButton.opacity = FadeButton.maxOpacity;
				if (musicPlayer != null) {
					musicPlayer.skipNext();
				}
			}
		});

		// add a button for the user to open a folder
		openFolder.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent ev) {
				openFolder.opacity = FadeButton.maxOpacity;
				if (lastPickedFolder != null) {
					folderChooser.setInitialDirectory(lastPickedFolder);
				}
				openFolder(folderChooser.showDialog(stage));
			}
		});

		// add an area for the user to seek through the song
		clickSeek.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent ev) {
				clickSeek.opacity = FadeButton.maxOpacity;
				double seekRatio = ev.getSceneX() / canvas.getWidth();
				seekRatio = Double.min(1.0, seekRatio);
				seekRatio = Double.max(0.0, seekRatio);
				if (musicPlayer != null) {
					musicPlayer.seek(seekRatio);
				}
			}
		});
		clickSeek.setOnMouseDragged(clickSeek.getOnMouseClicked());

		// add a button for the user to favorite a song
		favorite.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent ev) {
				favorite.opacity = FadeButton.maxOpacity;
				if (musicPlayer != null) {
					musicPlayer.favorite();
					;
				}
			}
		});

		// align all the elements
		canvas.widthProperty().bind(mainPane.widthProperty());
		canvas.heightProperty().bind(mainPane.heightProperty());

		prevButton.widthProperty().bind(mainPane.widthProperty().subtract(20).multiply(0.3));
		prevButton.heightProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.8));
		prevButton.yProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.1).add(10));

		playButton.widthProperty().bind(mainPane.widthProperty().subtract(20).multiply(0.4));
		playButton.heightProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.8));
		playButton.xProperty().bind(mainPane.widthProperty().subtract(20).multiply(0.3).add(10));
		playButton.yProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.1).add(10));

		nextButton.widthProperty().bind(mainPane.widthProperty().subtract(20).multiply(0.3));
		nextButton.heightProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.8));
		nextButton.xProperty().bind(mainPane.widthProperty().subtract(20).multiply(0.7).add(20));
		nextButton.yProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.1).add(10));

		openFolder.heightProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.1));
		openFolder.widthProperty().bind(mainPane.widthProperty().subtract(30).multiply(0.25));

		neverAgain.heightProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.1));
		neverAgain.widthProperty().bind(mainPane.widthProperty().subtract(30).multiply(0.25));
		neverAgain.xProperty().bind(mainPane.widthProperty().subtract(30).multiply(0.25).add(10));

		favorite.heightProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.1));
		favorite.widthProperty().bind(mainPane.widthProperty().subtract(30).multiply(0.25));
		favorite.xProperty().bind(mainPane.widthProperty().subtract(30).multiply(0.5).add(20));

		fullScreen.heightProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.1));
		fullScreen.widthProperty().bind(mainPane.widthProperty().subtract(30).multiply(0.25));
		fullScreen.xProperty().bind(mainPane.widthProperty().subtract(30).multiply(0.75).add(30));

		clickSeek.heightProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.1));
		clickSeek.widthProperty().bind(mainPane.widthProperty().multiply(1.0));
		clickSeek.yProperty().bind(mainPane.heightProperty().subtract(20).multiply(0.9).add(20));

		// construct the player
		innerPane.add(new Pane());
		innerPane.getLast().getChildren().add(canvas);

		innerPane.add(new Pane());
		innerPane.getLast().getChildren().addAll(buttons);

		mainPane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
		mainPane.getChildren().addAll(innerPane);

		Scene scene = new Scene(mainPane);

		// hide the mouse after 6 seconds
		PauseTransition idle = new PauseTransition(Duration.seconds(6));
		idle.setOnFinished(e -> scene.setCursor(Cursor.NONE));
		scene.addEventHandler(Event.ANY, e -> {
			idle.playFromStart();
			scene.setCursor(Cursor.DEFAULT);
		});

		stage.setScene(scene);
		stage.show();
		timer.start();

		// open the last played folder
		String lastPlayed = preferences.get("lastPlayed", "");
		if (!lastPlayed.equals("")) {
			openFolder(new File(lastPlayed));
		}

	}

	/**
	 * Opens a folder and attempt to play songs from it.
	 * @param folder The folder to search.
	 */
	private void openFolder(File folder) {
		if (folder == null || !folder.exists()) {
			return;
		} else {
			lastPickedFolder = folder.getParentFile();
		}

		musicPlayer = new MusicPlayer(folder, this);
		if (musicPlayer.isValid()) {
			musicPlayer.play();
			preferences.put("lastPlayed", folder.getAbsolutePath());
		} else {
			musicPlayer = null;
		}
	}

	/**
	 * Handle resizing.
	 */
	private void redraw() {
		graphics.setFill(Color.BLACK);
		graphics.fillRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
		for (int i = 0; i < bandCount; i++) {
			xs[i] = (int) (((double) i * canvas.getWidth()) / ((double) bandCount));
			widths[i] = (int) (((double) (i + 1) * canvas.getWidth()) / ((double) bandCount)) - xs[i];
		}
		BassBall.radius = Double.min(canvas.getWidth(), canvas.getHeight()) * 0.02;
		blur.setHeight(BassBall.radius * 0.5);
		blur.setWidth(BassBall.radius * 0.5);
	}

	/**
	 * Set various media player parameters.
	 */
	protected void initMediaPlayer(MediaPlayer mp) {
		mp.setAudioSpectrumNumBands(bandCount);
		mp.setAudioSpectrumInterval(1.0 / 12.0);
		mp.setAudioSpectrumListener(visual);
		mp.setStopTime(Duration.INDEFINITE);
		mp.setOnEndOfMedia(playNext);
		mp.setOnError(playNext);
	}

	/**
	 * Set the title of the current window.
	 */
	protected void setTitle(String title) {
		stage.setTitle(title);
	}
}
