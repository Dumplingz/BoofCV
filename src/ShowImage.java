import java.awt.image.BufferedImage;

import boofcv.alg.color.ColorHsv;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

public class ShowImage {

  public static void main(String[] args) {
    BufferedImage image = UtilImageIO
        .loadImage("imgs/goal.png");

    // Convert input image into a BoofCV HSV image
    Planar<GrayF32> rgb = ConvertBufferedImage.convertFromMulti(image, null,
        true, GrayF32.class);
    Planar<GrayF32> hsv = rgb.createSameShape();
    ColorHsv.rgbToHsv_F32(rgb, hsv);

    ColorHsv.hsvToRgb_F32(hsv, rgb);
    ShowImages.showWindow(hsv, "hsv");

  }

}
