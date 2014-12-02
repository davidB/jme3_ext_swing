package jme3_ext_swing;

import java.awt.Component;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import com.jme3.app.Application;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;

//https://github.com/caprica/vlcj-javafx/blob/master/src/test/java/uk/co/caprica/vlcj/javafx/test/JavaFXDirectRenderingTest.java
//http://stackoverflow.com/questions/15951284/javafx-image-resizing
//http://hub.jmonkeyengine.org/forum/topic/offscreen-rendering-problem/
//TODO manage suspend/resume (eg when image/stage is hidden)
public class SceneProcessorCopyToImagePanel implements SceneProcessor {

	private RenderManager rm;
	private ViewPort latestViewPorts;
	private int askWidth  = 1;
	private int askHeight = 1;
	private boolean askFixAspect = true;
	private TransfertImage timage;
	private AtomicBoolean reshapeNeeded  = new AtomicBoolean(true);

	private ImagePanel imgView;
	private ComponentListener listener = new ComponentListener() {

		@Override
		public void componentShown(ComponentEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void componentResized(ComponentEvent e) {
			Component c = e.getComponent();
			SceneProcessorCopyToImagePanel.this.componentResized(c.getWidth(), c.getHeight(), true);
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void componentHidden(ComponentEvent e) {
			// TODO Auto-generated method stub

		}
	};

	public void componentResized(int w, int h, boolean fixAspect) {
		int newWidth2 = Math.max(w, 1);
		int newHeight2 = Math.max(h, 1);
		if (askWidth != newWidth2 || askWidth != newHeight2 || askFixAspect != fixAspect){
			askWidth = newWidth2;
			askHeight = newHeight2;
			askFixAspect = fixAspect;
			reshapeNeeded.set(true);
		}
	}

	public void bind(final ImagePanel view, Application jmeApp){
		unbind();

		if (jmeApp != null) {
			List<ViewPort> vps = jmeApp.getRenderManager().getPostViews();
			latestViewPorts = vps.get(vps.size() - 1);
			latestViewPorts.addProcessor(this);
		}

		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				imgView = view;
				if (imgView != null) {
					imgView.addComponentListener(listener);
					componentResized(imgView.getWidth(), imgView.getHeight(), true);
				}
			}
		});
	}

	public void unbind(){
		if (latestViewPorts != null){
			latestViewPorts.removeProcessor(this); // call this.cleanup()
			latestViewPorts = null;
		}

		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				if (imgView != null) {
					imgView.removeComponentListener(listener);
				}
			}
		});
	}

	@Override
	public void initialize(RenderManager rm, ViewPort vp) {
		if (this.rm == null){
			// First time called in OGL thread
			this.rm = rm;
		}
	}

	private TransfertImage reshapeInThread(int width0, int height0, boolean fixAspect) {
		TransfertImage ti = new TransfertImage(width0, height0);

		rm.getRenderer().setMainFrameBufferOverride(ti.fb);
		rm.notifyReshape(ti.width, ti.height);

		//		for (ViewPort vp : viewPorts){
		//			vp.getCamera().resize(ti.width, ti.height, fixAspect);
		//
		//			// NOTE: Hack alert. This is done ONLY for custom framebuffers.
		//			// Main framebuffer should use RenderManager.notifyReshape().
		//			for (SceneProcessor sp : vp.getProcessors()){
		//				sp.reshape(vp, ti.width, ti.height);
		//			}
		//		}
		return ti;
	}

	@Override
	public boolean isInitialized() {
		return timage != null;
	}

	@Override
	public void preFrame(float tpf) {
	}

	@Override
	public void postQueue(RenderQueue rq) {
	}

	@Override
	public void postFrame(FrameBuffer out) {
		if (imgView != null && timage != null) {
			//		if (out != timage.fb){
			//			throw new IllegalStateException("Why did you change the output framebuffer? " + out + " != " + timage.fb);
			//		}
			timage.copyFrameBufferToImage(rm, imgView);
		}
		// for the next frame
		if (reshapeNeeded.getAndSet(false)){
			timage = reshapeInThread(askWidth, askHeight, askFixAspect);
			//TODO dispose previous timage ASAP (when no longer used in JavafFX thread)
		}
	}

	@Override
	public void cleanup() {
		if (timage != null) {
			timage.dispose();
			timage = null;
		}
	}

	@Override
	public void reshape(ViewPort vp, int w, int h) {
	}

	static class TransfertImage {
		public final int width;
		public final int height;
		public final FrameBuffer fb;
		public final ByteBuffer byteBuf;
		public final byte[] array;
		public final BufferedImage img;
		private ImagePanel lastIv = null;
		private AtomicBoolean invoked = new AtomicBoolean(false);
		public  SinglePixelPackedSampleModel sm;



		static final int BGRA_size = 8 * 4; // format of image returned by  readFrameBuffer (ignoring format in framebuffer.color
		static final int[] bOffs = {2, 1, 0, 3};

		TransfertImage(int width, int height) {
			this.width = width;
			this.height = height;
			fb = new FrameBuffer(width, height, 1);
			fb.setDepthBuffer(Format.Depth);
			fb.setColorBuffer(Format.ABGR8);
			byteBuf = BufferUtils.createByteBuffer(width * height * BGRA_size);
			array = new byte[width*height*4];
			byteBuf.get(array);
			byteBuf.position(0);
			DataBufferByte dbb = new DataBufferByte(array, array.length);
			//DataBufferByte dbb = new DataBufferByte(byteBuf.array(), );
			//			WritableRaster raster = Raster.createInterleavedRaster(dbb,
			//					width, height,
			//					width * format.getBitsPerPixel(),
			//					format.getBitsPerPixel(),
			//					new int []{ 0, 1, 2 },
			//					(Point)null);
			//			final int[] bitMasks = {0xff0000, 0xff00, 0xff, 0xff000000};
			//			sm = new SinglePixelPackedSampleModel(dbb.getDataType(), width*4, height, bitMasks);
			//			int[] bm = sm.getBitMasks();
			//
			//			//sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, width, height, bitMasks);
			//			final WritableRaster raster = Raster.createWritableRaster(sm, dbb, null);
			//			ColorModel colorModel =  new ComponentColorModel(
			//					ColorSpace.getInstance(ColorSpace.CS_sRGB),
			//					new int[] { 8, 8, 8 },
			//					false,
			//					false,
			//					Transparency.OPAQUE,
			//					DataBuffer.TYPE_BYTE);
			//			ComponentSampleModel cm = new ComponentSampleModel(DataBuffer.TYPE_BYTE,width,height,3,width*3,new int[] {2,1,0});
			//			final ColorModel cm = new DirectColorModel(
			//			        32, 0xff0000, 0xff00, 0xff, 0xff000000);
			//			final ColorModel cm = new DirectColorModel(format.getBitsPerPixel(), bitMasks[0], bitMasks[1], bitMasks[2], bitMasks[3]);
			//format.getBitsPerPixel()
			//			final ColorModel cm = new DirectColorModel(32, bitMasks[0], bitMasks[1], bitMasks[2], bitMasks[3]);
			//			img = new BufferedImage(cm, raster, false, null);
			//			img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

			//			   img = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
			//			   sm = new ComponentSampleModel(DataBuffer.TYPE_BYTE,width,height,3,width*3,new int[] {0,1,2});
			//			   Raster raster = Raster.createRaster(sm,dbb,null);
			//			   img.setData(raster);

			ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

			int[] nBits = {8, 8, 8,8};

			ColorModel cm = new ComponentColorModel(cs, nBits, true, false,
					Transparency.TRANSLUCENT,
					DataBuffer.TYPE_BYTE);
			int psize = BGRA_size / DataBuffer.getDataTypeSize(dbb.getDataType());
			WritableRaster raster = Raster.createInterleavedRaster(
					dbb,
					width,height,width*psize,psize, bOffs, null);

			img = new BufferedImage(cm,raster,false,null);
			//img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		}

		/** SHOULD run in JME'Display thread */
		void copyFrameBufferToImage(RenderManager rm, final ImagePanel iv) {
			synchronized (byteBuf) {
				byteBuf.clear();
				rm.getRenderer().readFrameBuffer(fb, byteBuf);
			}
			if(!invoked.compareAndSet(false, true)) return;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					synchronized (byteBuf) {
						if (lastIv != iv) {
							lastIv = iv;
							lastIv.image = img;
						}
						invoked.set(false);
						//IntBuffer intBuf = byteBuf.order(ByteOrder.BIG_ENDIAN).asIntBuffer();
						//int[] array = new int[intBuf.remaining()];
						//intBuf.get(array);
						//byteBuf.array(), byteBuf.capacity()
						//byte[] array = ((DataBufferByte)img.getData().getDataBuffer()).getData();
						//byte[] array = new byte[byteBuf.remaining()];
						//byte[] array = new byte[width*height*4];
						//System.out.printf(" pos : %d / rem : %d / limit : %d \n", byteBuf.position(), byteBuf.remaining(), byteBuf.limit());
						byteBuf.position(0);
						//byteBuf.get(array);
						//copy + Y flip data
						for(int y = 0; y < height; y++){
							for(int x = 0; x < width; x++){
								int pixel = ((height-y-1)*width + x) * 4;
								array[pixel + 0] = byteBuf.get();
								array[pixel + 1] = byteBuf.get();
								array[pixel + 2] = byteBuf.get();
								array[pixel + 3] = byteBuf.get();
							}
						}
						iv.repaint();
					}
				}
			});
		}

		void dispose() {
			fb.dispose();
		}
	}
}