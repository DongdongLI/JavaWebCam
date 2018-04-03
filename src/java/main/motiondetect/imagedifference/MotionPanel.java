package motiondetect.imagedifference;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

public class MotionPanel extends JPanel implements Runnable{

	private static final int DELAY = 100;
	private static final int DELAY_BEFORE_SHUTTIN_DOWN = 500;
	private static final int DEFAULT_CAMERA_ID =1;
	private static final int MIN_MOVE_REPORT = 3; 
	private static final int HEIGHT = 640;
	private static final int WEIGHT = 480;
	
	private volatile boolean isRunning;
	private volatile boolean isFinished;

	private Point precCogPoint;
	private Point cogPoint;
	private BufferedImage crosshairs;
	
	OpenCVFrameConverter.ToIplImage converter;
	Java2DFrameConverter frameConverter;
	IplImage image;
	
	public MotionPanel(){
		converter = new OpenCVFrameConverter.ToIplImage();
		frameConverter = new Java2DFrameConverter();
		
		setBackground(Color.white);
		crosshairs = loadImage("crosshairs.png");
		new Thread(this).start(); 
	}
	
	
	// Mandatory. If not the panel will be shrinked (no size)
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WEIGHT, HEIGHT);
	}
	
	@Override
	public void run() {
		FrameGrabber grabber = initGrabber();
		if(grabber == null)
			return;
		
		image = grabPicture(grabber, DEFAULT_CAMERA_ID);
		CVMotionDetector motionDetector = new CVMotionDetector(image);
		//JCVMotionDetector motionDetector = new JCVMotionDetector(image);
		Point pt;
		
		long duration;
		int snapCount = 0;
		
		isRunning = true;
		isFinished = false;
		
		while(isRunning) {
			image = grabPicture(grabber, DEFAULT_CAMERA_ID);
			
			motionDetector.calculateMovie(image);
			//motionDetector.calcMove(image);
			pt = motionDetector.getCOG();
			//pt = motionDetector.findCOG(image);
			if(pt != null) {
				precCogPoint = cogPoint;
				cogPoint = pt;
				reportCOGChanges(cogPoint, precCogPoint);
			}
			
			repaint();
			
			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		closeGrabber(grabber, DEFAULT_CAMERA_ID);
		System.out.println("Terminated.");
		isFinished = true;
	}
	
	private void reportCOGChanges(Point cogPoint, Point precCogPoint) {
		// TODO Auto-generated method stub
		if(precCogPoint == null) return;
		
		int xStep = cogPoint.x() - precCogPoint.x();
		int yStep = -1 * (cogPoint.y() - precCogPoint.y());
		
		int distance = (int)Math.round(Math.sqrt(xStep*xStep + yStep*yStep ));
		
		int angle = (int) Math.round(
				Math.toDegrees(
						Math.atan2(yStep, xStep)));
		
		if(distance > MIN_MOVE_REPORT) {
			System.out.println("COG: ("+cogPoint.x()+", "+cogPoint.y()+")");
			System.out.println("Distance Moved: "+distance+", angle: "+angle);
		}
	}


	private FrameGrabber initGrabber() {
		FrameGrabber grabber = null;
				
		try {
			grabber = FrameGrabber.createDefault(DEFAULT_CAMERA_ID);
			grabber.setFormat("dshow");
			
			grabber.setImageHeight(HEIGHT);
			grabber.setImageWidth(WEIGHT);
			grabber.start();
		}
		catch (Exception e) {
			System.out.println("Could not start grabber");
			System.out.print(e);
			System.exit(-1);
		}
		
		return grabber;
	}
	
	private IplImage grabPicture(FrameGrabber grabber, int ID) {
//		Frame frame = null;
//		try {
//			frame = grabber.grab();
//		} catch(Exception e) {
//			System.out.println("Problem grabbing image for: "+ID);
//		}
//		return converter.convert(frame);
		//return CVMotionDetector.convertFrameToGrayScale(converter.convert(frame));
		
		IplImage im = null;
	    try {
	      im = converter.convert(grabber.grab());  // take a snap
	    }
	    catch(Exception e) 
	    {  System.out.println("Problem grabbing image for camera " + ID);  }
	    return im;
	}
	
	private void closeGrabber(FrameGrabber grabber, int ID) {
		try {
			grabber.close();
			grabber.release();
		} catch(Exception e) {
			System.out.println("Problem releasing grabber for: "+ID);
		}
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(image != null) {
			g.setColor(Color.YELLOW);
			g.drawImage( frameConverter.getBufferedImage( converter.convert(image)) ,0 ,0, this);
			
			if(cogPoint != null) {
				// if there is a cog, draw cross bar
				drawCrosshairs(g, cogPoint.x(), cogPoint.y());
			}
			
			g.setColor(Color.YELLOW);
		}
		else {
			g.setColor(Color.BLUE);
			g.drawString("Loading from camera "+DEFAULT_CAMERA_ID+"...", 5, HEIGHT-10);
		}
	}
	
	private BufferedImage loadImage(String fileName) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(fileName));
			System.out.println("Reading file "+fileName+"...");
		}catch(Exception e) {
			System.out.println("File not found.");
		}
		
		return img;
	}
	
	private void drawCrosshairs(Graphics g, int x, int y) {
		if(crosshairs != null ) {
			g.drawImage(crosshairs, x - crosshairs.getWidth()/2, y - crosshairs.getHeight()/2, this);
		}
		else {
			g.setColor(Color.RED);
			g.fillOval(x-10, y-10, 20, 20);
		}
	}
	
	public void closeDown() {
		isRunning = false;
		while(!isFinished) {
			try {
				Thread.sleep(DELAY_BEFORE_SHUTTIN_DOWN);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
