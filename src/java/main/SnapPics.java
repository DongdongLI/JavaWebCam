import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.helper.opencv_objdetect;

public class SnapPics extends JFrame{
	private Snapper snapper;
	public SnapPics() {
						
		Container container = getContentPane();
		container.setLayout(new BorderLayout());
		
		snapper = new Snapper();
		container.add(snapper, BorderLayout.CENTER);
		
		Loader.load(opencv_objdetect.class);
		
		addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if(keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER)
					snapper.takeSnap();
			}
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				snapper.closeDown();
				System.exit(0);;
			}
		});
		
		setResizable(false);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public static void main(String[] args) {
		new SnapPics();
	}
}
