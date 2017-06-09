package MusicPlayer;

//loudness = brightness?
//base = shake+speed?

final class BassBall {
	protected static double radius = 8.0;
	protected static double diameter = radius * 2.0;
	protected double direction = 0.0;
	protected static double opacity = 0.0;
	protected double x = 0.5;
	protected double y = 0.5;

	protected BassBall(double direction) {
		this.direction = direction;
	}

}
