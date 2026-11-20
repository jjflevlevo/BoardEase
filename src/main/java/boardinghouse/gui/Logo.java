package boardinghouse.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

// The BoardEase logo.
// Save the logo image as logo.png (or logo.jpg) in src/main/resources.
// If the file is missing, a drawn "BoardEase" wordmark is shown instead.
public class Logo extends JComponent {
    private static boolean loaded = false;
    private static BufferedImage source;   // trimmed logo image (null when no file was found)
    private static boolean transparent;    // true when the file already has a transparent background

    private final BufferedImage rendered;  // prepared at 2x size so it stays sharp

    public Logo(int width) {
        loadSource();
        int height;
        if (source != null) {
            height = Math.max(1, Math.round((float) source.getHeight() * width / source.getWidth()));
            rendered = render(width * 2, height * 2);
        } else {
            height = Math.round(width * 0.26f);
            rendered = null;
        }
        Dimension d = new Dimension(width, height);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // small square icon for the window and taskbar
    public static Image appIcon() {
        int s = 64;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.ACCENT);
        g.fillRoundRect(0, 0, s, s, 18, 18);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD | Font.ITALIC, 42));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("B", (s - fm.stringWidth("B")) / 2, (s - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
        return img;
    }

    // ---------- Painting ----------
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (rendered != null) g2.drawImage(rendered, 0, 0, getWidth(), getHeight(), null);
        else drawWordmark(g2);
        g2.dispose();
    }

    // used when the logo file is missing: "Board" in white, "Ease" in blue
    private void drawWordmark(Graphics2D g2) {
        float size = getHeight();
        Font f = new Font("Segoe UI", Font.BOLD | Font.ITALIC, Math.round(size));
        FontMetrics fm = g2.getFontMetrics(f);
        float fit = (float) getWidth() / Math.max(1, fm.stringWidth("BoardEase"));
        f = f.deriveFont(size * fit * 0.98f);
        fm = g2.getFontMetrics(f);

        int y = (getHeight() - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent();
        g2.setFont(f);
        g2.setColor(Theme.TEXT);
        g2.drawString("Board", 0, y);
        g2.setColor(Theme.ACCENT);
        g2.drawString("Ease", fm.stringWidth("Board"), y);
    }

    // ---------- Loading the image ----------
    private static synchronized void loadSource() {
        if (loaded) return;
        loaded = true;
        BufferedImage raw = readFile();
        if (raw != null) prepare(raw);
    }

    private static BufferedImage readFile() {
        for (String name : new String[]{"logo.png", "logo.jpg", "logo.jpeg"}) {
            try (InputStream in = Logo.class.getResourceAsStream("/" + name)) {
                if (in != null) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) return img;
                }
            } catch (IOException ignored) { }

            for (String folder : new String[]{"src/main/resources/", ""}) {
                File f = new File(folder + name);
                if (f.isFile()) {
                    try {
                        BufferedImage img = ImageIO.read(f);
                        if (img != null) return img;
                    } catch (IOException ignored) { }
                }
            }
        }
        return null;
    }

    // crops the empty margins and checks whether the background is transparent
    private static void prepare(BufferedImage raw) {
        int w = raw.getWidth();
        int h = raw.getHeight();
        BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.drawImage(raw, 0, 0, null);
        g.dispose();
        int[] px = argb.getRGB(0, 0, w, h, null, 0, w);

        boolean hasAlpha = false;
        for (int p : px) {
            if ((p >>> 24) < 200) {
                hasAlpha = true;
                break;
            }
        }

        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = px[y * w + x];
                boolean visible;
                if (hasAlpha) {
                    visible = (p >>> 24) > 24;
                } else {
                    int red = (p >> 16) & 255;
                    int green = (p >> 8) & 255;
                    int blue = p & 255;
                    visible = Math.min(red, Math.min(green, blue)) < 235;
                }
                if (visible) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return; // nothing visible, keep the drawn wordmark

        // a white badge needs more breathing room than a transparent logo
        int contentHeight = maxY - minY + 1;
        int pad = hasAlpha ? Math.round(contentHeight * 0.08f) : Math.round(contentHeight * 0.35f);
        int x0 = Math.max(0, minX - pad);
        int y0 = Math.max(0, minY - pad);
        int x1 = Math.min(w - 1, maxX + pad);
        int y1 = Math.min(h - 1, maxY + pad);

        source = argb.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
        transparent = hasAlpha;
    }

    private static BufferedImage render(int w, int h) {
        BufferedImage scaled = scale(source, w, h);
        if (transparent) return scaled;

        // white background: round the corners so it looks like a badge
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }

    // shrinks in halves first, which keeps big images smooth
    private static BufferedImage scale(BufferedImage src, int targetW, int targetH) {
        BufferedImage current = src;
        while (current.getWidth() / 2 >= targetW) {
            current = step(current, current.getWidth() / 2, Math.max(1, current.getHeight() / 2));
        }
        return step(current, targetW, targetH);
    }

    private static BufferedImage step(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}