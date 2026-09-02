import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

public class IconGen {
    static final Color BG_TOP   = new Color(0x1a1d23);
    static final Color BG_BOT   = new Color(0x101216);
    static final Color BORDER   = new Color(0x2a2f37);
    static final Color BOX_LINE = new Color(0x55636f);
    static final Color BOX_FILL = new Color(0x4caf50);
    static final Color CROSS    = new Color(0x262c34);
    static final Color HIT      = new Color(0x4caf50);
    static final Color MISS     = new Color(0xef5350);

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "icon.png";
        int size = args.length > 1 ? Integer.parseInt(args[1]) : 256;

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double s = size / 256.0;

        double r = 48 * s;
        Shape card = new RoundRectangle2D.Double(0, 0, size, size, r, r);
        g.setClip(card);
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, size, BG_BOT));
        g.fill(card);

        double cx = size / 2.0, cy = size / 2.0;

        g.setColor(CROSS);
        g.setStroke(new BasicStroke((float) (2 * s)));
        g.draw(new Line2D.Double(cx, 24 * s, cx, size - 24 * s));
        g.draw(new Line2D.Double(24 * s, cy, size - 24 * s, cy));

        double boxW = 62 * s, boxH = 186 * s;
        Shape box = new Rectangle2D.Double(cx - boxW / 2, cy - boxH / 2, boxW, boxH);
        g.setColor(new Color(BOX_FILL.getRed(), BOX_FILL.getGreen(), BOX_FILL.getBlue(), 20));
        g.fill(box);
        g.setColor(BOX_LINE);
        g.setStroke(new BasicStroke((float) (2.5 * s)));
        g.draw(box);

        Random rng = new Random(20260727L);
        double dot = 7.0 * s;

        for (int i = 0; i < 165; i++) {
            boolean inner = i < 128;
            double sx = inner ? 14 * s : 30 * s;
            double sy = inner ? 38 * s : 66 * s;
            double x = cx + rng.nextGaussian() * sx;
            double y = cy + rng.nextGaussian() * sy;
            if (x < 8 * s || x > size - 8 * s || y < 8 * s || y > size - 8 * s) {
                continue;
            }
            boolean insideBox = box.contains(x, y);
            boolean landed = insideBox ? rng.nextDouble() < 0.86 : rng.nextDouble() < 0.10;
            Color c = landed ? HIT : MISS;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), landed ? 235 : 225));
            g.fill(new Ellipse2D.Double(x - dot / 2, y - dot / 2, dot, dot));
        }

        g.setClip(null);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke((float) (3 * s)));
        g.draw(new RoundRectangle2D.Double(1.5 * s, 1.5 * s, size - 3 * s, size - 3 * s, r, r));

        g.dispose();
        File f = new File(out);
        ImageIO.write(img, "PNG", f);
        System.out.println("wrote " + f.getAbsolutePath() + " (" + size + "x" + size + ")");
    }
}
