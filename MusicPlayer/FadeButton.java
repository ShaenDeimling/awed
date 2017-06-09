package MusicPlayer;

import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Represents a simple button that glows when clicked.
 */
final class FadeButton extends Rectangle {
	/**
	 * The opacity immediately after clicked.
	 */
	static protected double maxOpacity = 0.1;
	
	/**
	 * The current opacity.
	 */
	protected double opacity = 0.0;
	
	/**
	 * The button's tooltip.
	 */
	protected Tooltip tt = null;
	
	/**
	 * The button's color.
	 */
	protected Color color = Color.color(0.3, 0.3, 0.3);
	
	/**
	 * Create a button with a tooltip.
	 */
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
	
	/**
	 * Create a colored button with a tooltip.
	 * @param tooltip
	 * @param c
	 */
	protected FadeButton(String tooltip, Color c) {
		this(tooltip);
		color = c;
	}
	
	/**
	 * Set this button's tooltip.
	 * @param tooltip The new tooltip.
	 */
	protected void setTooltip(String tooltip) {
		Tooltip.uninstall(this, tt);
		tt = new Tooltip(tooltip);
		Tooltip.install(this, tt);
	}
	
	static protected void setMaxOpacity(double opacity) {
		maxOpacity = opacity;
	}
}
