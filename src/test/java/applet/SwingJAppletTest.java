package applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JApplet;
import javax.swing.JFrame;

import jme3_ext_swing.Function;
import jme3_ext_swing.ImagePanel;
import jme3_ext_swing.JmeForImagePanel;
import samples.CameraDriverAppState;
import samples.CameraDriverInput;
import samples.HelloPicking;

import com.jme3.app.SimpleApplication;

public class SwingJAppletTest extends JApplet {
	private static final long serialVersionUID = 1L;
	private boolean fullscreen;
	private JFrame frame;
	private Container parent;
	private Dimension size;

	public SwingJAppletTest() {
	}
	public void init() {
		//Execute a job on the event-dispatching thread:
		//creating this applet's GUI.
		try {
			javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					createGUI();
				}
			});
		} catch (Exception e) {
			System.err.println("createGUI didn't successfully complete");
		}
	}

	private void createGUI() {
		final ImagePanel imagePanel = new ImagePanel();
		imagePanel.setBackground(Color.GREEN);
		imagePanel.setSize(600,400);
		imagePanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				super.mouseEntered(e);
				e.getComponent().requestFocus();
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				toggleFullScreen();
			}
		});
		final JmeForImagePanel jme = new JmeForImagePanel();
		jme.bind(imagePanel);
		jme.enqueue(new Function<SimpleApplication, Boolean>() {
			@Override
			public Boolean apply(SimpleApplication t) {
				//return createScene(t);
				t.getStateManager().attach(new HelloPicking(imagePanel));
				t.getStateManager().attach(new CameraDriverAppState());

				CameraDriverInput driver = new CameraDriverInput();
				driver.jme = t;
				driver.speed = 1.0f;
				CameraDriverInput.bindDefaults(imagePanel, driver);
				return true;
			}
		});
		getContentPane().add(imagePanel, BorderLayout.CENTER);
	}

	public void toggleFullScreen() {
		if (!this.fullscreen) {
			this.size = this.getSize();
			if (this.parent == null) {
				this.parent = getParent();
			}
			this.frame = new JFrame();
			this.frame.setUndecorated(true);
			this.frame.add(this);
			this.frame.setVisible(true);

			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] devices = ge.getScreenDevices();
			devices[0].setFullScreenWindow(this.frame);
			this.fullscreen = true;
		} else {
			if (this.parent != null) {
				this.parent.add(this);
			}
			if (this.frame != null) {
				this.frame.dispose();
			}
			this.fullscreen = false;
			this.setSize(size);
			this.revalidate();
		}
		this.requestFocus();
	}
}