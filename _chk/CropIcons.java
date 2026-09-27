import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 把 hero_icons.png 的第 row 行（16px 一帧 ×8 列）放大 8 倍另存，便于肉眼核对帧内容。 */
public class CropIcons {
	public static void main(String[] args) throws Exception {
		File in = new File("core/src/main/assets/interfaces/hero_icons.png");
		BufferedImage src = ImageIO.read(in);
		System.out.println("size = " + src.getWidth() + " x " + src.getHeight()
				+ "  ⇒ 列 " + (src.getWidth() / 16) + " × 行 " + (src.getHeight() / 16));
		int row = Integer.parseInt(args[0]);
		int scale = args.length > 1 ? Integer.parseInt(args[1]) : 8;
		BufferedImage out = new BufferedImage(16 * 8 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
		for (int fy = 0; fy < 1; fy++) {
			for (int fx = 0; fx < 8; fx++) {
				for (int y = 0; y < 16; y++) {
					for (int x = 0; x < 16; x++) {
						int px = src.getRGB(fx * 16 + x, (row + fy) * 16 + y);
						for (int sy = 0; sy < scale; sy++) {
							for (int sx = 0; sx < scale; sx++) {
								out.setRGB((fx * 16 + x) * scale + sx, (fy * 16 + y) * scale + sy, px);
							}
						}
					}
				}
			}
		}
		ImageIO.write(out, "png", new File("_chk/icons_row" + row + ".png"));
		System.out.println("wrote _chk/icons_row" + row + ".png");
	}
}
