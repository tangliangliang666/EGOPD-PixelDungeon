import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 按帧号裁剪 hero_icons.png（16px/帧，8 列）并放大，便于肉眼核对。用法：FrameCrop <frameIndex> [scale] */
public class FrameCrop {
	public static void main(String[] args) throws Exception {
		BufferedImage src = ImageIO.read(new File("core/src/main/assets/interfaces/hero_icons.png"));
		int cols = src.getWidth() / 16;
		int idx = Integer.parseInt(args[0]);
		int scale = args.length > 1 ? Integer.parseInt(args[1]) : 12;
		int fx = idx % cols, fy = idx / cols;
		System.out.println("frame " + idx + " -> col " + fx + " row " + fy);
		BufferedImage out = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
		int opaque = 0;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int px = src.getRGB(fx * 16 + x, fy * 16 + y);
				if ((px >>> 24) > 8) opaque++;
				for (int sy = 0; sy < scale; sy++) {
					for (int sx = 0; sx < scale; sx++) {
						out.setRGB(x * scale + sx, y * scale + sy, px);
					}
				}
			}
		}
		System.out.println("opaque pixels = " + opaque + " / 256");
		ImageIO.write(out, "png", new File("_chk/frame_" + idx + ".png"));
		System.out.println("wrote _chk/frame_" + idx + ".png");
	}
}
