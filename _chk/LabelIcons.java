import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** 把 hero_icons.png 第 17 行第 1~4 帧（128~131）放大并加 ASCII 标签，便于核对槽位分配。 */
public class LabelIcons {
	public static void main(String[] args) throws Exception {
		BufferedImage src = ImageIO.read(new File("core/src/main/assets/interfaces/hero_icons.png"));
		int cols = src.getWidth() / 16;
		int[] frames = {128, 129, 130, 131};
		String[] labels = {"128 talent", "129 GRIT", "130 NEVER", "131 EXEC"};
		int scale = 10, pad = 6, labelH = 18;
		int cw = 16 * scale;
		BufferedImage out = new BufferedImage(frames.length * cw + pad, cw + labelH + pad, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setColor(new Color(0x20, 0x20, 0x28));
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
		for (int i = 0; i < frames.length; i++) {
			int f = frames[i], fx = f % cols, fy = f / cols;
			int ox = pad / 2 + i * cw;
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					int px = src.getRGB(fx * 16 + x, fy * 16 + y);
					g.setColor(new Color(px, true));
					g.fillRect(ox + x * scale, pad / 2 + y * scale, scale, scale);
				}
			}
			g.setColor(new Color(0xFF, 0xD0, 0x60));
			g.drawString(labels[i], ox + 2, pad / 2 + cw + 14);
		}
		g.dispose();
		ImageIO.write(out, "png", new File("_chk/mf_ability_icons.png"));
		System.out.println("wrote _chk/mf_ability_icons.png  " + out.getWidth() + "x" + out.getHeight());
	}
}
