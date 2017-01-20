import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;

import org.ddogleg.struct.FastQueue;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;

public class IsolateTape {

  /***
   * Filters out any values out side of the specified hues and lightnesses.
   * 
   * @param image
   *          an input of BufferedImage to filter the colors out from
   * @param minHueDegree
   *          minimum hue (in degrees from 0-360) allowed
   * @param maxHueDegree
   *          maximum hue (in degrees from 0-360) allowed
   * @param minValue
   *          minimum value, or lightness (0-255) allowed
   * @param maxValue
   *          maximum value, or lightness (0-255) allowed
   * @return returns a BufferedImage with all non-selected areas painted black
   */
  public static BufferedImage filterSelectedHSVColor(
      BufferedImage image, float minHueDegree,
      float maxHueDegree, float minValue, float maxValue) {

    // Make a planar image of the given image
    Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image, null,
        true, GrayF32.class);

    // Create a new image with the same image size
    Planar<GrayF32> hsv = input.createSameShape();

    // Convert into HSV
    ColorHsv.rgbToHsv_F32(input, hsv);

    // Extract hue and value bands which are independent of saturation
    GrayF32 H = hsv.getBand(0);
    GrayF32 V = hsv.getBand(2);

    // Makes a new BufferedImage that is completely black
    BufferedImage output = new BufferedImage(input.width, input.height,
        BufferedImage.TYPE_INT_RGB);

    // step through each pixel and find its hue and values
    for (int y = 0; y < hsv.height; y++) {
      for (int x = 0; x < hsv.width; x++) {
        // Get values for hue and value
        float dh = H.unsafe_get(x, y);
        float dv = V.unsafe_get(x, y);

        // Hue is an angle in radians, so simple subtraction doesn't
        // work
        float hueDegree = (float) ((dh * 180) / Math.PI);
        float brightnessValue = dv;

        // Test if each pixel is within the range of brightness and hue
        if (brightnessValue >= minValue && brightnessValue <= maxValue) {
          if (minHueDegree <= hueDegree && maxHueDegree >= hueDegree) {
            // If pixel is within the range, then add color back
            output.setRGB(x, y, image.getRGB(x, y));
          }
        }
      }
    }

    return output;
  }

  // Polynomial fitting tolerances
  static double splitFraction = 0.05;
  static double minimumSideFraction = 0.1;

  static ListDisplayPanel gui = new ListDisplayPanel();

  /**
   * Fits polygons to found contours around binary blobs.
   */
  public static void fitBinaryImage(GrayF32 input) {

    GrayU8 binary = new GrayU8(input.width, input.height);
    BufferedImage polygon = new BufferedImage(input.width, input.height,
        BufferedImage.TYPE_INT_RGB);

    // the mean pixel value is often a reasonable threshold when creating a
    // binary image
    double mean = ImageStatistics.mean(input);

    // create a binary image by thresholding
    ThresholdImageOps.threshold(input, binary, (float) mean, true);

    // reduce noise with some filtering
    GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
    filtered = BinaryImageOps.dilate8(filtered, 1, null);

    // Find the contour around the shapes
    List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT,
        null);

    // Fit a polygon to each shape and draw the results
    Graphics2D g2 = polygon.createGraphics();
    g2.setStroke(new BasicStroke(2));

    for (Contour c : contours) {
      // Fit the polygon to the found external contour. Note loop = true
      List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external,
          true, splitFraction, minimumSideFraction, 100);

      g2.setColor(Color.RED);
      VisualizeShapes.drawPolygon(vertexes, true, g2);

      // handle internal contours now
      g2.setColor(Color.BLUE);
      for (List<Point2D_I32> internal : c.internal) {
        vertexes = ShapeFittingOps.fitPolygon(internal, true, splitFraction,
            minimumSideFraction, 100);
        VisualizeShapes.drawPolygon(vertexes, true, g2);
      }
    }

    gui.addImage(polygon, "Binary Blob Contours");
  }

  /**
   * Fits a sequence of line-segments into a sequence of points found using the
   * Canny edge detector. In this case
   * the points are not connected in a loop. The canny detector produces a more
   * complex tree and the fitted
   * points can be a bit noisy compared to the others.
   */
  public static BufferedImage fitCannyEdges(GrayF32 input) {

    BufferedImage displayImage = new BufferedImage(input.width, input.height,
        BufferedImage.TYPE_INT_RGB);

    // Finds edges inside the image
    CannyEdge<GrayF32, GrayF32> canny = FactoryEdgeDetectors.canny(2, true,
        true, GrayF32.class, GrayF32.class);

    canny.process(input, 0.1f, 0.3f, null);
    List<EdgeContour> contours = canny.getContours();

    Graphics2D g2 = displayImage.createGraphics();
    g2.setStroke(new BasicStroke(2));

    // used to select colors for each line
    Random rand = new Random(234);

    for (EdgeContour e : contours) {
      g2.setColor(new Color(255));

      for (EdgeSegment s : e.segments) {
        // fit line segments to the point sequence. Note that loop is false
        List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(s.points,
            false,
            splitFraction, minimumSideFraction, 100);

        VisualizeShapes.drawPolygon(vertexes, false, g2);
      }
    }

    return displayImage;
  }

  /**
   * Detects contours inside the binary image generated by canny. Only the
   * external contour is relevant. Often
   * easier to deal with than working with Canny edges directly.
   */
  public static BufferedImage fitCannyBinary(GrayF32 input) {

    BufferedImage displayImage = new BufferedImage(input.width, input.height,
        BufferedImage.TYPE_INT_RGB);
    GrayU8 binary = new GrayU8(input.width, input.height);

    // Finds edges inside the image
    CannyEdge<GrayF32, GrayF32> canny = FactoryEdgeDetectors.canny(2, false,
        true, GrayF32.class, GrayF32.class);

    canny.process(input, 0.1f, 0.3f, binary);

    List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT,
        null);

    Graphics2D g2 = displayImage.createGraphics();
    g2.setStroke(new BasicStroke(2));

    // used to select colors for each line
    Random rand = new Random(234);

    for (Contour c : contours) {
      // Only the external contours are relevant.
      List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external,
          true,
          splitFraction, minimumSideFraction, 100);

      g2.setColor(new Color(rand.nextInt()));
      VisualizeShapes.drawPolygon(vertexes, true, g2);
    }

    return displayImage;
  }

  private static void processImages(String[] files,
      BinaryPolygonDetector<GrayU8> detector, ListDisplayPanel panel) {
    for (String fileName : files) {
      BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample(fileName));

      GrayU8 input = ConvertBufferedImage.convertFromSingle(image, null,
          GrayU8.class);
      GrayU8 binary = new GrayU8(input.width, input.height);

      // Binarization is done outside to allows creative tricks. For example,
      // when applied to a chessboard
      // pattern where square touch each other, the binary image is eroded first
      // so that they don't touch.
      // The squares are expanded automatically during the subpixel optimization
      // step.
      int threshold = GThresholdImageOps.computeOtsu(input, 0, 255);
      ThresholdImageOps.threshold(input, binary, threshold, true);

      // it takes in a grey scale image and binary image
      // the binary image is used to do a crude polygon fit, then the grey image
      // is used to refine the lines
      // using a sub-pixel algorithm
      detector.process(input, binary);

      // visualize results by drawing red polygons
      FastQueue<Polygon2D_F64> found = detector.getFoundPolygons();
      Graphics2D g2 = image.createGraphics();
      g2.setStroke(new BasicStroke(3));
      for (int i = 0; i < found.size; i++) {
        g2.setColor(Color.RED);
        VisualizeShapes.drawPolygon(found.get(i), true, g2, true);
        g2.setColor(Color.CYAN);
        VisualizeShapes.drawPolygonCorners(found.get(i), 2, g2, true);
      }

      panel.addImage(image, new File(fileName).getName());
    }
  }

  public static void main(String args[]) {
    // load and convert the image into a usable format
    BufferedImage origImage = UtilImageIO.loadImage("imgs/PSGoal.png");
    BufferedImage image = IsolateTape.filterSelectedHSVColor(origImage, 100f,
        180f,
        70f, 255f);
    GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null,
        GrayF32.class);
    GrayU8 blurred = new GrayU8(input.width, input.height);

    GrayU8 modifiedInput = ConvertBufferedImage.convertFromSingle(image, null,
        GrayU8.class);

    BlurImageOps.gaussian(modifiedInput, blurred, -1, 5, null);
    gui.addImage(blurred, "Original");

    ConvertImage.convert(blurred, input);

    gui.addImage(fitCannyEdges(input), "hi");
    gui.addImage(fitCannyBinary(input), "Canny Contour");
    fitBinaryImage(input);

    ShowImages.showWindow(gui, "Polygon from Contour", true);
  }

}
