import java.awt.image.BufferedImage;

import boofcv.alg.color.ColorHsv;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import georegression.metric.UtilAngle;

public class IsolateTape {

  private static BufferedImage image = UtilImageIO.loadImage("imgs/goal2.png");

  public static void main(String[] args) {
    showSelectedColor("image", image, 40);
  }

  public static void showSelectedColor(String name, BufferedImage image,
      float value) {
    Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image, null,
        true, GrayF32.class);
    Planar<GrayF32> hsv = input.createSameShape();

    // Convert into HSV
    ColorHsv.rgbToHsv_F32(input, hsv);

    // Euclidean distance squared threshold for deciding which pixels are
    // members of the selected set
    float maxDist1 = 33;
    float minDist1 = 30;
    float maxDist2 = 70;

    // Extract hue and value bands which are independent of saturation
    GrayF32 H = hsv.getBand(0);
    GrayF32 V = hsv.getBand(2);
    // Adjust the relative importance of Hue and Saturation.

    // step through each pixel and mark how close it is to the selected
    // color
    BufferedImage output = new BufferedImage(input.width, input.height,
        BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < hsv.height; y++) {
      for (int x = 0; x < hsv.width; x++) {
        // Hue is an angle in radians, so simple subtraction doesn't
        // work
        float dh = UtilAngle.dist(H.unsafe_get(x, y), value);
        float dv = UtilAngle.dist(V.unsafe_get(x, y), value);

        // this distance measure is a bit naive, but good enough for to
        // demonstrate the concept
        float dist = dh;
        float dist2 = dv;
        if (dist2 >= maxDist2) {
          if (minDist1 <= dist && maxDist1 >= dist) {
            output.setRGB(x, y, image.getRGB(x, y));
          }
        }
      }
    }

    ShowImages.showWindow(output, "Showing " + name);
  }

}
