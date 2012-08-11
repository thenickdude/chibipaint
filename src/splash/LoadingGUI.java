package splash;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.Timer;

public class LoadingGUI extends JComponent {

	private static final long serialVersionUID = 1L;

	// Note, access only through getters/setters for synchronization
	private double progress = 0;

	private String message = null;

	private static final Color[] hlight = { new Color(41, 71, 110), new Color(50, 79, 116), new Color(62, 90, 124),
			new Color(76, 101, 134), new Color(93, 116, 145), new Color(110, 131, 158), new Color(129, 146, 169),
			new Color(148, 163, 183), new Color(167, 179, 195), new Color(186, 195, 208), new Color(204, 210, 220),
			new Color(220, 225, 230), new Color(234, 236, 241), new Color(245, 247, 249) };

	private static final int BARRADIUS = hlight.length;

	private static final int TEXTMARGIN = 8;

	private volatile boolean imagesReady = false;
	
	private Image cup, lid, lines, text;
	private BufferedImage shading, highlights, smoothie;
	private BufferedImage smoothieComposite;

	private int smoothieOffset = 0;

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
	
	private static BufferedImage imageToBuffered(Image src) {
		BufferedImage result = new BufferedImage(src.getWidth(null), src.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		
		Graphics g = result.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
		
		return result;
	}
	
	/**
	 * Shift the given source image downwards by a certain number of pixels, then intersect it with its transparency
	 * mask from its original position.
	 * 
	 * @param image
	 * @param result
	 * @param shiftImageDown
	 */
	private static void shiftImageDownAndMaskWithOriginalTransparency(BufferedImage image, BufferedImage result, int shiftImageDown) {
	    int width = image.getWidth();
	    int height = image.getHeight();
	    int[] imagePixels;

	    if (shiftImageDown < image.getHeight()) {
			imagePixels = image.getRGB(0, 0, width, height, null, 0, width);
	
		    for (int i = imagePixels.length - 1; i >= shiftImageDown * width; i--)
		    {
		    	int srcPixelIndex = i - width * shiftImageDown;
		    	
		    	int color = imagePixels[srcPixelIndex] & 0x00ffffff; // Mask preexisting alpha
		    	
		        int alpha = (((imagePixels[i] >>> 24) * (imagePixels[srcPixelIndex] >>> 24)) & 0xFF00) << 16;
		        
		        imagePixels[i] = color | alpha;
		    }
		    Arrays.fill(imagePixels, 0, width * shiftImageDown, 0);
		} else {
			imagePixels = new int[width * height];
		}

	    result.setRGB(0, 0, width, height, imagePixels, 0, width);
	}

	public LoadingGUI() {
		super();

		setOpaque(true); // we paint all our bits

		cup = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("cup.png"));
		lid = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("lid.png"));
		lines = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("lines.png"));
		final Image shading_comp = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("shading.png"));
		final Image highlights_comp = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("highlights.png"));
		final Image smoothie_comp = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("smoothie.png"));
		text = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("text.png"));
		
		final MediaTracker tracker = new MediaTracker(this);
		
		tracker.addImage(cup, 0);
		tracker.addImage(highlights_comp, 1);
		tracker.addImage(lid, 2);
		tracker.addImage(lines, 3);
		tracker.addImage(shading_comp, 4);
		tracker.addImage(smoothie_comp, 5);
		tracker.addImage(text, 6);
		
		new Thread(new Runnable() {
			
			public void run() {
				try {
					tracker.waitForAll();
				} catch (InterruptedException e) {
				}
				
				imagesReady = !tracker.isErrorAny();
				
				if (imagesReady) {
					/* 
					 * Highlights are in a compressed format that our compositor can't handle, so convert
					 * those now to ARGB.
					 */
					
					shading = imageToBuffered(shading_comp);
					highlights = imageToBuffered(highlights_comp);
					smoothie = imageToBuffered(smoothie_comp);
					
					smoothieComposite = new BufferedImage(smoothie.getWidth(null), smoothie.getHeight(null), BufferedImage.TYPE_INT_ARGB);
				}
				
				final Timer timer = new javax.swing.Timer(5, null);
				
				final long startTime = System.currentTimeMillis();
				
				timer.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						smoothieOffset = (int) ((System.currentTimeMillis() - startTime) / 50);
						repaint();
						
						timer.restart();
					}
				});
				
				timer.start();
				
				repaint();
		    }
		}).start();
		
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		/*
		 * make a copy to keep graphics state the same
		 */
		Graphics2D g2d = (Graphics2D) graphics.create();

		g2d.setColor(Color.white);
		g2d.fillRect(0, 0, getWidth(), getHeight());

		int centerX, centerY;

		centerX = getWidth() / 2;
		centerY = getHeight() / 2 - 40;

		if (imagesReady) {
			int imgWidth = text.getWidth(null);
			int imgHeight = text.getHeight(null);
			
			g2d.drawImage(text, centerX - imgWidth / 2, centerY - imgHeight /2, this);
			
			BufferedImage cupComposite = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
			
			Graphics2D cupCompG = (Graphics2D) cupComposite.getGraphics();
			{
				cupCompG.drawImage(cup, 0, 0, this);
				
				shiftImageDownAndMaskWithOriginalTransparency(smoothie, smoothieComposite, smoothieOffset);
				
				cupCompG.drawImage(smoothieComposite, 0, 0, this);
				
				cupCompG.drawImage(lid, 0, 0, this);

				cupCompG.setComposite(BlendComposite.Screen);
				cupCompG.drawImage(highlights, 0, 0, this);

				cupCompG.setComposite(BlendComposite.Multiply);
				cupCompG.drawImage(shading, 0, 0, this);				
			}

			Composite mainNormalComposite = g2d.getComposite();

			//The whole cup composite is slightly transparent
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.88f));
			g2d.drawImage(cupComposite, centerX - imgWidth / 2, centerY - imgHeight /2, this);
			
			g2d.setComposite(mainNormalComposite);
					
			g2d.drawImage(lines, centerX - imgWidth / 2, centerY - imgHeight /2, this);

			centerY += imgHeight / 2 + 20;
		}

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
					g2d.drawString(message, lastX - messageWidth - TEXTMARGIN, centerY + messageHeight / 2 - 2);
				} else {
					g2d.setColor(Color.black);
					g2d.drawString(message, lastX + TEXTMARGIN, centerY + messageHeight / 2 - 2);
				}

			}
		}

		g2d.dispose();
	}
}
