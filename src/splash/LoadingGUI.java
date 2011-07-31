package splash;

import java.net.*;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

public class LoadingGUI extends JComponent implements ImageObserver {

	private static final long serialVersionUID = 1L;

	private Image loadingImage;

	private int imgWidth = -1, imgHeight = -1;

	// Note, access only through getters/setters for synchronization
	private double progress = 0;

	private String message = null;

	private static final Color[] hlight = { new Color(41, 71, 110),
			new Color(50, 79, 116), new Color(62, 90, 124),
			new Color(76, 101, 134), new Color(93, 116, 145),
			new Color(110, 131, 158), new Color(129, 146, 169),
			new Color(148, 163, 183), new Color(167, 179, 195),
			new Color(186, 195, 208), new Color(204, 210, 220),
			new Color(220, 225, 230), new Color(234, 236, 241),
			new Color(245, 247, 249) };

	private static final int BARRADIUS = hlight.length;

	private static final int TEXTMARGIN = 8;

	/**
	 * Only call from within the Swing event dispatcher thread!
	 *
	 * @param message
	 *            Message to display on the loading bar.
	 */
	public void setMessage(String message) {
		this.message = message;
		repaint();
	}

	public void setProgress(double progress) {
		this.progress = Math.min(Math.max(progress, 0), 1);
		repaint();
	}

	public void setMessageProgress(String message, double progress) {
		this.message = message;
		this.progress = Math.min(Math.max(progress, 0), 1);
		repaint();
	}

	private String getMessage() {
		return message;
	}

	private double getProgress() {
		return progress;
	}

	public LoadingGUI() {
		super();

		setOpaque(true); // we paint all our bits

		//Start loading the splash screen in the background...
		URL url = this.getClass().getResource("loadingscreen.png");
		Image image = Toolkit.getDefaultToolkit().getImage(url);

		imgWidth = image.getWidth(this);
		imgHeight = image.getHeight(this);

		if (imgWidth!=-1 && imgHeight!=-1) {
			//We've loaded this image already
			loadingImage = image;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics) {

		/*
		 * make a copy to keep graphics state the same
		 */
		Graphics2D g2d = (Graphics2D) graphics.create();

		g2d.setColor(Color.white);
		g2d.fillRect(0, 0, getWidth(), getHeight());

		if (loadingImage != null) {

			int centerX, centerY;

			centerX = (getWidth() - imgWidth) / 2;
			centerY = (getHeight() - imgHeight) / 2;

			g2d.drawImage(loadingImage, centerX, centerY, this);

			centerY += 90;

			int messageWidth = 0;
			int messageHeight = 0;

			String message = getMessage();

			if (message != null) {
				messageWidth = g2d.getFontMetrics().stringWidth(message);
				messageHeight = g2d.getFontMetrics().getHeight();
			}

			int lastX = Math.round((float) (getProgress() * getWidth()));

			for (int y = centerY - BARRADIUS + 1; y <= centerY + BARRADIUS - 1; y++) {

				g2d.setColor(hlight[Math.abs(y - centerY)]);
				g2d.drawLine(0, y, lastX, y);

				int c = 255 - (BARRADIUS - Math.abs(y - centerY)) * 7;
				g2d.setColor(new Color(c, c, c));
				g2d.drawLine(lastX + 1, y, getWidth() - 1, y);

				if (message != null) {
					if (messageWidth > getWidth() - lastX - 2 * TEXTMARGIN) {
						g2d.setColor(Color.white);
						g2d.drawString(message, lastX - messageWidth
								- TEXTMARGIN, centerY + messageHeight / 2 - 2);
					} else {
						g2d.setColor(Color.black);
						g2d.drawString(message, lastX + TEXTMARGIN, centerY
								+ messageHeight / 2 - 2);
					}

				}
			}
		}

		g2d.dispose();
	}

	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height) {

		if ((infoflags & ImageObserver.WIDTH) != 0) {
			imgWidth = width;
		}
		if ((infoflags & ImageObserver.HEIGHT) != 0) {
			imgHeight = height;
		}

		if (imgWidth != -1 && imgHeight != -1) {
			loadingImage = img;
			repaint();
			return true;
		} else
			return false;
	}
}
