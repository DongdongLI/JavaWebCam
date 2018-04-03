package motiondetect.imagedifference;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.helper.opencv_objdetect;

public class MotionDetector extends JFrame{

	private MotionPanel motionPanel;
	
	public MotionDetector() {
		super("Motion Detector");
		
		Container container = getContentPane();
		container.setLayout(new BorderLayout());
		
		Loader.load(opencv_objdetect.class);
		
		motionPanel = new MotionPanel();
		container.add(motionPanel, BorderLayout.CENTER);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				motionPanel.closeDown();
				System.exit(0);
			}
		});
		
		setResizable(false);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new MotionDetector();
	}
}
