package motiondetect.MixtureOfGaussians;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.helper.opencv_objdetect;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.opencv.video.BackgroundSubtractorMOG2;

import static org.bytedeco.javacpp.opencv_core.*;

public class MogCog {

	private static final int DELAY = 100;
	private static CvMemStorage contourStorage;
	
	public static void main(String[] args) throws Exception {
		Loader.load(opencv_objdetect.class);
		
		contourStorage = CvMemStorage.create();
		
		System.out.println("Initializing frame grabber");
		OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(1);

		grabber.start();
		
		Frame grab = grabber.grab();
		
		OpenCVFrameConverter.ToIplImage converter = new ToIplImage();
		IplImage image = converter.convert(grab);
		
		int width = image.width();
		int height = image.height();
		IplImage foregroundMask = IplImage.create(width, height, IPL_DEPTH_8U, 1);
		
		IplImage background = IplImage.create(width, height, IPL_DEPTH_8U, 3);
		
		CanvasFrame grabCanvas = new CanvasFrame("Camera");
		grabCanvas.setLocation(0,0);
		CanvasFrame mogCanvas = new CanvasFrame("MOG Info");
		mogCanvas.setLocation(width+5, 0);
		
		BackgroundSubtractorMOG2 mog = new BackgroundSubtractorMOG2(300, 16, false);
		mog.set("nmixtures", 3);
		System.out.println("Number of mixtures: "+mog.getVarInit("nmixtures"));
		System.out.println("Shadow detect: "+mog.getDetectShadows());
		
		try {
			System.out.println("Background ratio: "+mog.getBackgroundRatio());
		}
		catch(RuntimeException e) {
			System.out.println(e);
		}
		
		while(grabCanvas.isVisible() && mogCanvas.isVisible()) {
			long startTime = System.currentTimeMillis();
			image = converter.convert(grabber.grab());
			if(image == null) {
				System.out.println("Image grab failed");
				break;
			}
			
			mog.apply(image, fgmask, learningRate);
		}
	}
}
