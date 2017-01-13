import java.awt.image.BufferedImage;

import boofcv.alg.color.ColorHsv;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

public class IsolateTape {

	public static void showSelectedColor(String name, BufferedImage image, float hue, float minHue, float maxHue,
			float minValue, float maxValue) {
		Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image, null, true, GrayF32.class);
		Planar<GrayF32> hsv = input.createSameShape();

		// Convert into HSV
		ColorHsv.rgbToHsv_F32(input, hsv);

		// Extract hue and value bands which are independent of saturation
		GrayF32 H = hsv.getBand(0);
		GrayF32 V = hsv.getBand(2);
		// Adjust the relative importance of Hue and Saturation.

		// step through each pixel and mark how close it is to the selected
		// color
		BufferedImage output = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < hsv.height; y++) {
			for (int x = 0; x < hsv.width; x++) {
				// Hue is an angle in radians, so simple subtraction doesn't
				// work
				float dh = H.unsafe_get(x, y);
				float dv = V.unsafe_get(x, y);

				// this distance measure is a bit naive, but good enough for to
				// demonstrate the concept
				float hueDist = (float) ((dh * 180) / Math.PI);
				float valueDist = dv;

				// System.out.println(dist);

				if (valueDist >= minValue && valueDist <= maxValue) {
					if (minHue <= hueDist && maxHue >= hueDist) {
						output.setRGB(x, y, image.getRGB(x, y));
					}
				}
			}
		}

		ShowImages.showWindow(output, "Showing " + name);
	}

}
