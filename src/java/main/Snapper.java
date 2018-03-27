import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JPanel;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

public class Snapper extends JPanel implements Runnable{

	private static final int DELAY = 100;
	private static final int DELAY_BEFORE_SHUTTIN_DOWN = 500;
	private static final int DEFAULT_CAMERA_ID =1;
	private static final int HEIGHT = 640;
	private static final int WEIGHT = 480;
	
	private volatile boolean isRunning;
	private volatile boolean isFinished;
	private volatile boolean takeCurrentPic;
	
	OpenCVFrameConverter.ToIplImage converter;
	Java2DFrameConverter frameConverter;
	IplImage image;
	
	public Snapper(){
		setBackground(Color.white);
		new Thread(this).start(); // start updating the panel's image
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
		
		long duration;
		int snapCount = 0;
		
		isRunning = true;
		isFinished = false;
		takeCurrentPic = false;
		
		while(isRunning) {
			image = grabPicture(grabber, DEFAULT_CAMERA_ID);
			
			if(takeCurrentPic) {
				// save the photo
				takeCurrentPic = false;
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
	
	private FrameGrabber initGrabber() {
		FrameGrabber grabber = null;
		converter = new OpenCVFrameConverter.ToIplImage();
		frameConverter = new Java2DFrameConverter();
		
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
		Frame frame = null;
		try {
			frame = grabber.grab();
		} catch(Exception e) {
			System.out.println("Problem grabbing image for: "+ID);
		}
		return converter.convert(frame);
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
		}
		else {
			g.setColor(Color.BLUE);
			g.drawString("Loading from camera "+DEFAULT_CAMERA_ID+"...", 5, HEIGHT-10);
		}
	}
	
	public void takeSnap() {
		takeCurrentPic = true;
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
