import java.awt.image.BufferedImage;

import boofcv.alg.color.ColorHsv;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

public class ShowImage {

  /***
   * 
   * @param hsv
   */
  public static void showImage(Planar<GrayF32> hsv) {
    Planar<GrayF32> rgb = hsv.createSameShape();

    ColorHsv.hsvToRgb_F32(hsv, rgb);
    ShowImages.showWindow(rgb, "Showing HSV Image");
  }
  
  public static void showImage(BufferedImage image){
	  ShowImages.showWindow(image, "Showing Image");
  }

}
