import java.awt.image.BufferedImage;

import boofcv.io.image.UtilImageIO;

public class TestImage {
	public static void main(String[] args) {
		BufferedImage image = UtilImageIO.loadImage("imgs/goal.png");
		ShowImage.showImage(IsolateTape.filterSelectedHSVColor( image, 100f, 180f, 70f, 255f));
	}
}
