import java.awt.image.BufferedImage;

import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;

public class TestImage {
  public static void main(String[] args) {
    BufferedImage origImage = UtilImageIO.loadImage("imgs/goalRetake5.JPG");
    BufferedImage image = IsolateTape.filterSelectedHSVColor(origImage, 100f,
        180f, 70f, 255f);
    image = IsolateTape.convertToBinaryImage(image, 0);

    ListDisplayPanel panel = new ListDisplayPanel();

    // first configure the detector to only detect convex shapes with 3 to 7
    // sides
    ConfigPolygonDetector config = new ConfigPolygonDetector(3, 7);
    BinaryPolygonDetector<GrayU8> detector = FactoryShapeDetector
        .polygon(config, GrayU8.class);

    IsolateTape.processPolygons(image, detector, panel);

    // now lets detect concave shapes with many sides
    config.maximumSides = 12;
    config.convex = false;
    detector = FactoryShapeDetector.polygon(config, GrayU8.class);

    IsolateTape.processPolygons(image, detector, panel);

    ShowImages.showWindow(panel, "Found Polygons", true);
  }
}
