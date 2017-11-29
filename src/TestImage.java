import java.awt.image.BufferedImage;

import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;

public class TestImage {
  public static void main(String[] args) {
    ListDisplayPanel panel = new ListDisplayPanel();

    BufferedImage origImage = UtilImageIO.loadImage("imgs/goalRetake5.JPG");

    BufferedImage image = IsolateTape.getBinaryImage(origImage, 200);

    panel.addImage(origImage, "asdf1");
    panel.addImage(image, "asdf2");

    BufferedImage hsvImage = IsolateTape.filterSelectedHSVColor(origImage, 100f,
        180f, 70f, 255f);
    panel.addImage(hsvImage, "plswork1");

    hsvImage = IsolateTape.convertToBinaryImage(hsvImage, 0);
    panel.addImage(hsvImage, "plswork2");

    System.out.println(System.currentTimeMillis());
    hsvImage = IsolateTape.gaussianBlur(hsvImage, 20);
    System.out.println(System.currentTimeMillis());

    // // first configure the detector to only detect convex shapes with 3 to 7
    // // sides
    // ConfigPolygonDetector config = new ConfigPolygonDetector(3, 7);
    // BinaryPolygonDetector<GrayU8> detector = FactoryShapeDetector
    // .polygon(config, GrayU8.class);
    //
    // IsolateTape.processPolygons(image, detector, panel);
    //
    // // now lets detect concave shapes with many sides
    // config.maximumSides = 12;
    // config.convex = false;
    // detector = FactoryShapeDetector.polygon(config, GrayU8.class);
    //
    // IsolateTape.processPolygons(image, detector, panel);
    panel.addImage(hsvImage, "plswork3");

    hsvImage = IsolateTape.getCenterPoint(hsvImage);
    panel.addImage(hsvImage, "Find Center Point");

    ShowImages.showWindow(panel, "Found Polygons", true);
  }
}
