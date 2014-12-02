package samples;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;

import javax.swing.JFrame;
import javax.swing.JLabel;

import jme3_ext_swing.Function;
import jme3_ext_swing.ImagePanel;
import jme3_ext_swing.JmeForImagePanel;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

public class ImagePanelDemo {

	public static void main(String[] args){
		makeGUI().setVisible(true);
	}

	public static JFrame makeGUI(){
		JFrame mainFrame = new JFrame("Java Swing Examples");
		mainFrame.setSize(400,400);
		mainFrame.setLayout(new BorderLayout());
		JLabel headerLabel = new JLabel("", JLabel.CENTER);
		JLabel statusLabel = new JLabel("",JLabel.CENTER);

		statusLabel.setSize(350,100);

		final ImagePanel imagePanel = new ImagePanel();
		imagePanel.setBackground(Color.GREEN);
		imagePanel.setSize(600,400);
		//controlPanel.setLayout(new FlowLayout());

		final JmeForImagePanel jme = new JmeForImagePanel();
		jme.bind(imagePanel);
		jme.enqueue(new Function<SimpleApplication, Boolean>() {
			@Override
			public Boolean apply(SimpleApplication t) {
				//return createScene(t);
				t.getStateManager().attach(new HelloPicking(imagePanel));
				return true;
			}
		});
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowIconified(java.awt.event.WindowEvent e) {
				// TODO Auto-generated method stub
				jme.enqueue(new Function<SimpleApplication, Boolean>() {
					@Override
					public Boolean apply(SimpleApplication t) {
						t.loseFocus();
						return true;
					}
				});

			}

			@Override
			public void windowDeiconified(java.awt.event.WindowEvent e) {
				jme.enqueue(new Function<SimpleApplication, Boolean>() {
					@Override
					public Boolean apply(SimpleApplication t) {
						t.gainFocus();
						return true;
					}
				});
			}

			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				jme.stop(true);
			}
		});

		mainFrame.add(headerLabel, BorderLayout.NORTH);
		mainFrame.add(imagePanel, BorderLayout.CENTER);
		mainFrame.add(statusLabel, BorderLayout.SOUTH);
		return mainFrame;
	}

	/**
	 * Create a similar scene to Tutorial "Hello Material" but without texture
	 * http://hub.jmonkeyengine.org/wiki/doku.php/jme3:beginner:hello_material
	 *
	 * @param jmeApp the application where to create a Scene
	 */
	static boolean createScene(SimpleApplication jmeApp) {
		Node rootNode = jmeApp.getRootNode();
		AssetManager assetManager = jmeApp.getAssetManager();

		/** A simple textured cube -- in good MIP map quality. */
		Box cube1Mesh = new Box( 1f,1f,1f);
		Geometry cube1Geo = new Geometry("My Textured Box", cube1Mesh);
		cube1Geo.setLocalTranslation(new Vector3f(-3f,1.1f,0f));
		Material cube1Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		cube1Mat.setColor("Color", ColorRGBA.Blue);
		cube1Geo.setMaterial(cube1Mat);
		rootNode.attachChild(cube1Geo);

		/** A translucent/transparent texture, similar to a window frame. */
		Box cube2Mesh = new Box( 1f,1f,0.01f);
		Geometry cube2Geo = new Geometry("window frame", cube2Mesh);
		Material cube2Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		cube2Mat.setColor("Color", ColorRGBA.Brown);
		cube2Geo.setQueueBucket(Bucket.Transparent);
		cube2Geo.setMaterial(cube2Mat);
		rootNode.attachChild(cube2Geo);

		/** A bumpy rock with a shiny light effect.*/
		Sphere sphereMesh = new Sphere(32,32, 2f);
		Geometry sphereGeo = new Geometry("Shiny rock", sphereMesh);
		sphereMesh.setTextureMode(Sphere.TextureMode.Projected); // better quality on spheres
		TangentBinormalGenerator.generate(sphereMesh);           // for lighting effect
		Material sphereMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		sphereMat.setBoolean("UseMaterialColors",true);
		sphereMat.setColor("Diffuse",ColorRGBA.Pink);
		sphereMat.setColor("Specular",ColorRGBA.White);
		sphereMat.setFloat("Shininess", 64f);  // [0,128]
		sphereGeo.setMaterial(sphereMat);
		sphereGeo.setLocalTranslation(0,2,-2); // Move it a bit
		sphereGeo.rotate(1.6f, 0, 0);          // Rotate it a bit
		rootNode.attachChild(sphereGeo);

		/** Must add a light to make the lit object visible! */
		DirectionalLight sun = new DirectionalLight();
		sun.setDirection(new Vector3f(1,0,-2).normalizeLocal());
		sun.setColor(ColorRGBA.White);
		rootNode.addLight(sun);
		return true;
	}
}