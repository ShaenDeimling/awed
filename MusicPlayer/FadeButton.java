package MusicPlayer;
import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

final class FadeButton extends Rectangle {
	static protected double maxOpacity = 0.1;
	protected double opacity = 0.0;
	protected Tooltip tt = null;
	protected Color color = Color.color(0.3, 0.3, 0.3);
	
	protected FadeButton(String tooltip) {
		setFill(Color.TRANSPARENT);
		tt = new Tooltip(tooltip);
		Tooltip.install(this, tt);
		setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				opacity = maxOpacity;
			}
		});
	}
	
	protected FadeButton(String tooltip, Color c) {
		this(tooltip);
		color = c;
	}
	
	protected void setTooltip(String tooltip) {
		Tooltip.uninstall(this, tt);
		tt = new Tooltip(tooltip);
		Tooltip.install(this, tt);
	}
	
	static protected void setMaxOpacity(double opacity) {
		maxOpacity = opacity;
	}
}
