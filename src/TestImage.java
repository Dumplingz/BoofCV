import java.awt.image.BufferedImage;

import boofcv.io.image.UtilImageIO;

public class TestImage {
	public static void main(String[] args) {
		BufferedImage image = UtilImageIO.loadImage("imgs/goal4.png");
		IsolateTape.showSelectedColor("image", image, 90, 100f, 180f, 70f, 160f);
	}
}
