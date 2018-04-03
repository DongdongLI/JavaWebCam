package motiondetect.imagedifference;

import java.awt.Dimension;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_imgproc.CvMoments;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
/*
 * By comparing current frame and previous frame (the grayscale ones...)
 * 
 * if the intensity of a pixel is large enough, then we will marked it as 255, else 0*/
public class CVMotionDetector {

	private static final int LOW_THRESHOLD = 64;
	//Minimum number of pixels for GOC calculations
	private static final int MIN_PIXELS = 100;
	
	private static final int MAX_PTS = 5;
	
	private IplImage prevImg, curImg, diffImg;
	
	private Dimension imgDimension = null;
	
	private Point cogPoint = null;
	private Point[] cogPoints;
	private int ptIdx, totalPts;
	
	public CVMotionDetector(IplImage firstFrame) {
		if(firstFrame == null) {
			System.out.println("No frame to initialize the motion detector");
			System.exit(-1);
		}				
		
		imgDimension = new Dimension(firstFrame.width(), firstFrame.height());
		
		cogPoints = new Point[MAX_PTS];
	    ptIdx = 0;
	    totalPts = 0;
	    
		prevImg = convertFrameToGrayScale(firstFrame);
		curImg = null;
		diffImg = IplImage.create(prevImg.width(), prevImg.height(), IPL_DEPTH_8U, 1);
	}
	
	// getters
	public Dimension getSize() {
		return imgDimension;
	}
	
	public IplImage getCurImg() {
		return curImg;
	}
	
	public IplImage getPrevImg() {
		return prevImg;
	}
	
	public IplImage getDiffImg() {
		return diffImg;
	}
	
	public Point getCOG() {
		if(totalPts == 0)
			return null;
		
		int xTotal = 0;
		int yTotal = 0;
		for(int i=0;i<totalPts;i++) {
			xTotal += cogPoints[i].x();
			yTotal += cogPoints[i].y();
		}
		
		return new Point((int)xTotal/totalPts, (int)yTotal/totalPts);
	}
	public static IplImage convertFrameToGrayScale(IplImage img) {
		cvSmooth(img, img);
		IplImage grayImg = IplImage.create(img.width(), img.height(), IPL_DEPTH_8U, 1);
		cvCvtColor(img, grayImg, CV_BGR2GRAY);
		
		cvEqualizeHist(grayImg, grayImg);
		return grayImg;
	}
	
	public void calculateMovie(IplImage curFrame) {
		if(curFrame == null) {
			System.out.println("Current frame is null");
			return;
		}
		
		if(curImg != null)
			prevImg = curImg;
		
		curImg = convertFrameToGrayScale(curFrame);
		
		/*
		 * Calculate absolute diff between cur and prev
		 * large value means there's movement*/
		cvAbsDiff(curImg, prevImg, diffImg);
		
		// if less then 64, low, larger then 64, high. So either 0 or 255
		cvThreshold(diffImg, diffImg, LOW_THRESHOLD, 255, CV_THRESH_BINARY);
		
		Point point = findCOG(diffImg);
		if(point != null) {
			cogPoints[ptIdx] = point;
			ptIdx = (ptIdx+1)%MAX_PTS;
			if(totalPts<MAX_PTS)
				totalPts++;
		}
	}

	public Point findCOG(IplImage diffImg) {
		Point point = null;
		// diffImg is a bi-level grayscale
		int numOfPixels = cvCountNonZero(diffImg);
		if(numOfPixels > MIN_PIXELS) {
			CvMoments moments = new CvMoments();
			
			// 1 means treat it as binary
			cvMoments(diffImg, moments, 1);

			double m00 = cvGetSpatialMoment(moments, 0, 0);
			double m10 = cvGetSpatialMoment(moments, 1, 0);
			double m01 = cvGetSpatialMoment(moments, 0, 1);
			
			if(m00 != 0) {
				int xCenter = (int)Math.round(m10/m00);
				int yCenter = (int)Math.round(m01/m00);
				point = new Point(xCenter, yCenter);
			}
		}
		
		return point;
		
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("Initializing frame grabber...");
	    OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(1);
	    
	    OpenCVFrameConverter.ToIplImage frameConverter = new OpenCVFrameConverter.ToIplImage();
	    
	    grabber.start();

	    frameConverter.convert(grabber.grab());
	    
	    CVMotionDetector md = new CVMotionDetector( frameConverter.convert(grabber.grab()) );

	    Dimension imgDim = md.getSize();
	    IplImage imgWithCOG = IplImage.create(imgDim.width, imgDim.height, IPL_DEPTH_8U, 1); 

	    // canvases for the image+COG and difference images
	    CanvasFrame cogCanvas = new CanvasFrame("Camera + COG");
	    cogCanvas.setLocation(0, 0);

	    CanvasFrame diffCanvas = new CanvasFrame("Difference");
	    diffCanvas.setLocation(imgDim.width+5, 0);

	    // display grayscale+COG and difference images, until a window is closed
	    while (cogCanvas.isVisible() && diffCanvas.isVisible())  {
		    long startTime = System.currentTimeMillis();
	      md.calculateMovie( frameConverter.convert(grabber.grab()) );

	      // show current grayscale image with COG drawn onto it
	      cvCopy(md.getCurImg(), imgWithCOG);
	      Point cogPoint = md.getCOG();
	      if (cogPoint != null)
	        cvCircle(imgWithCOG, cvPoint(cogPoint.x(), cogPoint.y()), 8,      // radius of circle
	                  CvScalar.WHITE, CV_FILLED, CV_AA, 0);     // line type, shift
	      	      
	      cogCanvas.showImage(frameConverter.convert(imgWithCOG));

	      diffCanvas.showImage(frameConverter.convert(md.getDiffImg()));   // show difference image
	      //System.out.println("Processing time: " + (System.currentTimeMillis() - startTime));
	    }

	    grabber.stop();
	    cogCanvas.dispose();
	    diffCanvas.dispose();
	}
}
