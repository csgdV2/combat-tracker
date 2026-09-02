import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import java.util.function.BiConsumer;

public class ExampleCharts {
    static final Color BG      = new Color(0x14161a);
    static final Color PANEL   = new Color(0x161a20);
    static final Color LINE    = new Color(0x2a2f37);
    static final Color GRID    = new Color(0x21262e);
    static final Color AXIS_TX = new Color(0x828c99);
    static final Color TITLE   = new Color(0x9fb3c8);
    static final Color MUTED   = new Color(0x8a93a0);
    static final Color HIT     = new Color(0x4caf50);
    static final Color MISS    = new Color(0xef5350);
    static final Color COMBO   = new Color(0x29b6f6);
    static final Color WARN    = new Color(0xffb454);

    static final int W = 1280, H = 470;
    static final int PW = 610, PH = 330;
    static final int PX1 = 20, PX2 = 650, PY = 96;

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();

        jumpReset(new File(dir, "example-jump-reset.png"));
        combo(new File(dir, "example-combo.png"));
        reach(new File(dir, "example-reach.png"));
        aim(new File(dir, "example-aim.png"));
        System.out.println("done");
    }

    static void jumpReset(File out) throws Exception {
        render(out, "Jump reset timing",
                "Milliseconds between taking the hit and jumping. Spread is what a person looks like.",
                "Human", "Timing wanders. SD 24 ms",
                "Macro", "Same value every time. SD 2 ms",
                (g, side) -> {
                    Random r = new Random(side == 0 ? 7 : 9);
                    int px = side == 0 ? PX1 : PX2;
                    axes(g, px, "ms", 0, 100, new String[]{"0", "25", "50", "75", "100"});
                    for (int i = 0; i < 90; i++) {
                        double v = side == 0
                                ? 46 + r.nextGaussian() * 24
                                : 41 + r.nextGaussian() * 2;
                        v = Math.max(0, Math.min(100, v));
                        double x = px + 46 + (PW - 62) * (i / 89.0);
                        double y = PY + PH - 34 - (v / 100.0) * (PH - 56);
                        dot(g, x, y, v >= 0 && v <= 80 ? HIT : MISS, 3.6);
                    }
                });
    }

    static void combo(File out) throws Exception {
        render(out, "Combo timing",
                "Gap between consecutive hits in a combo. Measured from your mouse press, not the tick.",
                "Human", "Clicks jitter. SD 54 ms",
                "Autoclicker", "Machine steady. SD 3 ms",
                (g, side) -> {
                    Random r = new Random(side == 0 ? 3 : 11);
                    int px = side == 0 ? PX1 : PX2;
                    axes(g, px, "ms", 450, 850, new String[]{"450", "550", "650", "750", "850"});
                    double prevX = -1, prevY = -1;
                    for (int i = 0; i < 70; i++) {
                        double v = side == 0
                                ? 645 + r.nextGaussian() * 54
                                : 627 + r.nextGaussian() * 3;
                        v = Math.max(450, Math.min(850, v));
                        double x = px + 46 + (PW - 62) * (i / 69.0);
                        double y = PY + PH - 34 - ((v - 450) / 400.0) * (PH - 56);
                        if (prevX >= 0) {
                            g.setColor(new Color(COMBO.getRed(), COMBO.getGreen(), COMBO.getBlue(), 90));
                            g.setStroke(new BasicStroke(1.2f));
                            g.draw(new Line2D.Double(prevX, prevY, x, y));
                        }
                        dot(g, x, y, COMBO, 3.6);
                        prevX = x; prevY = y;
                    }
                });
    }

    static void reach(File out) throws Exception {
        render(out, "Reach",
                "Distance from your eye to the target hitbox. The dashed line is vanilla range, 3.0 blocks.",
                "Human", "Hits stay under 3.0",
                "Reach cheat", "Hits land past 3.0",
                (g, side) -> {
                    Random r = new Random(side == 0 ? 5 : 13);
                    int px = side == 0 ? PX1 : PX2;
                    axes(g, px, "blocks", 0, 5, new String[]{"0", "1.25", "2.5", "3.75", "5"});

                    double refY = PY + PH - 34 - (3.0 / 5.0) * (PH - 56);
                    g.setColor(WARN);
                    g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10f, new float[]{5f, 5f}, 0f));
                    g.draw(new Line2D.Double(px + 46, refY, px + PW - 16, refY));
                    g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    g.drawString("vanilla 3.0", px + PW - 82, (int) refY - 5);

                    for (int i = 0; i < 95; i++) {
                        boolean landed = r.nextDouble() < 0.7;
                        double v;
                        if (side == 0) {
                            v = landed ? Math.min(3.0, 2.2 + r.nextGaussian() * 0.34)
                                       : 3.0 + Math.abs(r.nextGaussian()) * 0.5;
                        } else {
                            v = landed ? 3.35 + r.nextGaussian() * 0.22
                                       : 4.1 + Math.abs(r.nextGaussian()) * 0.3;
                        }
                        v = Math.max(0.3, Math.min(5, v));
                        double x = px + 46 + (PW - 62) * (i / 94.0);
                        double y = PY + PH - 34 - (v / 5.0) * (PH - 56);
                        dot(g, x, y, landed ? HIT : MISS, 3.6);
                    }
                });
    }

    static void aim(File out) throws Exception {
        render(out, "Aim placement",
                "Where your crosshair sat on the target hitbox, drawn to scale.",
                "Human", "Spread over the body",
                "Aim assist", "Pinned to dead centre",
                (g, side) -> {
                    Random r = new Random(side == 0 ? 17 : 23);
                    int px = side == 0 ? PX1 : PX2;
                    double cx = px + PW / 2.0, cy = PY + PH / 2.0;
                    double scale = (PH - 70) / 1.9;

                    g.setColor(GRID);
                    g.setStroke(new BasicStroke(1f));
                    g.draw(new Line2D.Double(cx, PY + 14, cx, PY + PH - 14));
                    g.draw(new Line2D.Double(px + 20, cy, px + PW - 20, cy));

                    double bw = 0.3 * scale, bh = 0.9 * scale;
                    Shape box = new Rectangle2D.Double(cx - bw, cy - bh, bw * 2, bh * 2);
                    g.setColor(new Color(HIT.getRed(), HIT.getGreen(), HIT.getBlue(), 16));
                    g.fill(box);
                    g.setColor(new Color(0x6b7480));
                    g.setStroke(new BasicStroke(1.4f));
                    g.draw(box);

                    for (int i = 0; i < 150; i++) {
                        double ox = side == 0 ? r.nextGaussian() * 0.16 : r.nextGaussian() * 0.022;
                        double oy = side == 0 ? r.nextGaussian() * 0.44 : r.nextGaussian() * 0.05;
                        double x = cx + ox * scale, y = cy - oy * scale;
                        if (x < px + 16 || x > px + PW - 16 || y < PY + 14 || y > PY + PH - 14) {
                            continue;
                        }
                        boolean landed = box.contains(x, y) ? r.nextDouble() < 0.85 : r.nextDouble() < 0.2;
                        dot(g, x, y, landed ? HIT : MISS, 3.4);
                    }
                });
    }

    static void render(File out, String heading, String sub,
                       String leftName, String leftNote, String rightName, String rightNote,
                       BiConsumer<Graphics2D, Integer> draw) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(BG);
        g.fillRect(0, 0, W, H);

        g.setColor(new Color(0xe8eaed));
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.drawString(heading, PX1, 36);
        g.setColor(MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(sub, PX1, 58);

        panel(g, PX1, leftName, leftNote, HIT);
        panel(g, PX2, rightName, rightNote, MISS);

        Shape old = g.getClip();
        g.setClip(new Rectangle(PX1, PY, PW, PH));
        draw.accept(g, 0);
        g.setClip(new Rectangle(PX2, PY, PW, PH));
        draw.accept(g, 1);
        g.setClip(old);

        g.dispose();
        ImageIO.write(img, "PNG", out);
        System.out.println("wrote " + out.getName());
    }

    static void panel(Graphics2D g, int px, String name, String note, Color tag) {
        g.setColor(PANEL);
        g.fill(new RoundRectangle2D.Double(px, PY, PW, PH, 12, 12));
        g.setColor(LINE);
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Double(px, PY, PW, PH, 12, 12));

        g.setColor(tag);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString(name, px, PY - 24);
        g.setColor(MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(note, px, PY - 6);
    }

    static void axes(Graphics2D g, int px, String unit, double lo, double hi, String[] labels) {
        int x0 = px + 46, y0 = PY + PH - 34, x1 = px + PW - 16, y1 = PY + 14;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int i = 0; i < labels.length; i++) {
            double t = i / (double) (labels.length - 1);
            int y = (int) (y0 - t * (y0 - y1));
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Double(x0, y, x1, y));
            g.setColor(AXIS_TX);
            int tw = g.getFontMetrics().stringWidth(labels[i]);
            g.drawString(labels[i], x0 - 8 - tw, y + 4);
        }
        g.setColor(LINE);
        g.draw(new Line2D.Double(x0, y1, x0, y0));
        g.draw(new Line2D.Double(x0, y0, x1, y0));
        g.setColor(AXIS_TX);
        g.drawString(unit, x0 - 40, y1 - 2);
    }

    static void dot(Graphics2D g, double x, double y, Color c, double r) {
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 225));
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }
}
