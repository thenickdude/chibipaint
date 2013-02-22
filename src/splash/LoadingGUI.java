package splash;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JComponent;

public class LoadingGUI extends JComponent {

	private static final long serialVersionUID = 1L;

	// Note, access only through getters/setters for synchronization
	private double progress = 0;

	private String message = null;

	private volatile boolean imagesReady = false;
	private volatile boolean showImages = true;
	private volatile boolean finished = false;
	
	private Image cup, lid, lines, text;
	private BufferedImage shading, highlights, smoothie;
	private BufferedImage smoothieComposite;

	
	private static final int MAXSMOOTHIEOFFSET = 170;

	private Font statusFont;

	private BufferedImage cupComposite;
	
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
	
	public void setShowImages(boolean showImages) {
		this.showImages = showImages;
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
	 * @param source
	 * @param result
	 * @param shiftImageDown
	 */
	private static void shiftImageDownAndMaskWithOriginalTransparency(BufferedImage source, BufferedImage result, int shiftImageDown) {
	    int width = source.getWidth();
	    int height = source.getHeight();
	    int[] imagePixels;

	    if (shiftImageDown < source.getHeight()) {
			imagePixels = source.getRGB(0, 0, width, height, null, 0, width);
	
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
		final Image shading_compressed = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("shading.png"));
		final Image highlights_compressed = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("highlights.png"));
		final Image smoothie_compressed = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("smoothie.png"));
		text = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("text.png"));
		
		final MediaTracker tracker = new MediaTracker(this);
		
		tracker.addImage(cup, 0);
		tracker.addImage(highlights_compressed, 1);
		tracker.addImage(lid, 2);
		tracker.addImage(lines, 3);
		tracker.addImage(shading_compressed, 4);
		tracker.addImage(smoothie_compressed, 5);
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
					 * Highlights are in a 8-bit per pixel format that our compositor can't handle, so convert
					 * those now to ARGB.
					 */
					
					shading = imageToBuffered(shading_compressed);
					highlights = imageToBuffered(highlights_compressed);
					smoothie = imageToBuffered(smoothie_compressed);
					
					smoothieComposite = new BufferedImage(smoothie.getWidth(null), smoothie.getHeight(null), BufferedImage.TYPE_INT_ARGB);
				}
				
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

		if (imagesReady && showImages) {
			int imgWidth = text.getWidth(null);
			int imgHeight = text.getHeight(null);
			
			g2d.drawImage(text, centerX - imgWidth / 2, centerY - imgHeight /2, this);
			
			buildCupComposite(imgWidth, imgHeight, progress);

			Composite regularAlphaComposite = g2d.getComposite();

			//The whole cup composite is slightly transparent
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.88f));
			g2d.drawImage(cupComposite, centerX - imgWidth / 2, centerY - imgHeight /2, this);
			
			g2d.setComposite(regularAlphaComposite);
					
			g2d.drawImage(lines, centerX - imgWidth / 2, centerY - imgHeight /2, this);

			centerY += imgHeight / 2 + 2;
		}

		String message = getMessage();

		if (message != null) {
			g2d.setColor(Color.black);
			
			if (statusFont == null) {
				statusFont = getFont().deriveFont(16);
			}
			
			g2d.setFont(statusFont);
			
			int messageHeight = g2d.getFontMetrics().getHeight();

			for (String line : message.split("\n")) {
				int lineWidth = g2d.getFontMetrics().stringWidth(line);
				
				centerY += messageHeight;
				g2d.drawString(line, centerX - lineWidth / 2, centerY);
			}
		}

		g2d.dispose();
	}

	private BufferedImage buildCupComposite(int imgWidth, int imgHeight, double progress) {
		if (cupComposite == null) {
			cupComposite = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
		}
		
		if (!finished) {
			Graphics2D cupCompG = (Graphics2D) cupComposite.getGraphics();
			try {
				cupCompG.drawImage(cup, 0, 0, this);
								
				shiftImageDownAndMaskWithOriginalTransparency(smoothie, smoothieComposite, (int) Math.round(progress * MAXSMOOTHIEOFFSET));
				
				cupCompG.drawImage(smoothieComposite, 0, 0, this);
				
				cupCompG.drawImage(lid, 0, 0, this);
	
				cupCompG.setComposite(BlendComposite.Screen);
				cupCompG.drawImage(highlights, 0, 0, this);
	
				cupCompG.setComposite(BlendComposite.Multiply);
				cupCompG.drawImage(shading, 0, 0, this);				
			} finally {
				cupCompG.dispose();
			}
		}
		
		return cupComposite;
	}

	/**
	 * Loading is done, so the percentage bar won't move after this.
	 */
	public void finished() {
		if (!finished) {
			int imgWidth = text.getWidth(null);
			int imgHeight = text.getHeight(null);
			
			buildCupComposite(imgWidth, imgHeight, 1.0);
			finished = true;
			
			/* 
			 * Since the image won't change, this is the final composite and we won't have to recomposite.
			 * Discard the components which go into that composite.
			 */
			cup = null;
			smoothie = null;
			smoothieComposite = null;
			lid = null;
			highlights = null;
			shading = null;
		}
	}
}
