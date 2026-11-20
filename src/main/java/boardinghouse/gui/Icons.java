package boardinghouse.gui;

import java.awt.*;
import java.awt.geom.*;

// small line icons drawn with Java2D, so no image files are needed
public class Icons {
    private Icons() { }

    // draws an icon of size s x s at (x, y); icons are designed on an 18 x 18 grid
    public static void draw(Graphics g, String name, int x, int y, int s, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        g2.scale(s / 18.0, s / 18.0);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (name) {
            case "dashboard" -> {
                g2.draw(new RoundRectangle2D.Float(2, 2, 6, 6, 2, 2));
                g2.draw(new RoundRectangle2D.Float(10, 2, 6, 6, 2, 2));
                g2.draw(new RoundRectangle2D.Float(2, 10, 6, 6, 2, 2));
                g2.draw(new RoundRectangle2D.Float(10, 10, 6, 6, 2, 2));
            }
            case "rooms" -> {
                Path2D roof = new Path2D.Float();
                roof.moveTo(2, 9);
                roof.lineTo(9, 2.5f);
                roof.lineTo(16, 9);
                g2.draw(roof);
                Path2D walls = new Path2D.Float();
                walls.moveTo(4, 8);
                walls.lineTo(4, 15.5f);
                walls.lineTo(14, 15.5f);
                walls.lineTo(14, 8);
                g2.draw(walls);
                Path2D door = new Path2D.Float();
                door.moveTo(7.5f, 15.5f);
                door.lineTo(7.5f, 11);
                door.lineTo(10.5f, 11);
                door.lineTo(10.5f, 15.5f);
                g2.draw(door);
            }
            case "tenants" -> {
                g2.draw(new Ellipse2D.Float(5.5f, 2.5f, 7, 7));
                Path2D body = new Path2D.Float();
                body.moveTo(3, 16);
                body.quadTo(3, 11, 9, 11);
                body.quadTo(15, 11, 15, 16);
                g2.draw(body);
            }
            case "billing" -> {
                g2.draw(new RoundRectangle2D.Float(3.5f, 2, 11, 14, 2, 2));
                g2.draw(new Line2D.Float(6.5f, 6.5f, 11.5f, 6.5f));
                g2.draw(new Line2D.Float(6.5f, 9.5f, 11.5f, 9.5f));
                g2.draw(new Line2D.Float(6.5f, 12.5f, 9.5f, 12.5f));
            }
            case "payments" -> {
                g2.draw(new RoundRectangle2D.Float(2, 4, 14, 10, 2.5f, 2.5f));
                g2.draw(new Line2D.Float(2, 8, 16, 8));
                g2.draw(new Line2D.Float(4.5f, 11.5f, 8, 11.5f));
            }
            case "reports" -> {
                g2.draw(new Line2D.Float(4, 15, 4, 10));
                g2.draw(new Line2D.Float(9, 15, 9, 3.5f));
                g2.draw(new Line2D.Float(14, 15, 14, 7));
            }
            case "history" -> {
                g2.draw(new Ellipse2D.Float(2.5f, 2.5f, 13, 13));
                Path2D hands = new Path2D.Float();
                hands.moveTo(9, 5.5f);
                hands.lineTo(9, 9);
                hands.lineTo(11.5f, 10.5f);
                g2.draw(hands);
            }
            case "bell" -> {
                Path2D bell = new Path2D.Float();
                bell.moveTo(4, 13);
                bell.quadTo(5.5f, 12, 5.5f, 9);
                bell.curveTo(5.5f, 5.5f, 7, 4, 9, 4);
                bell.curveTo(11, 4, 12.5f, 5.5f, 12.5f, 9);
                bell.quadTo(12.5f, 12, 14, 13);
                bell.closePath();
                g2.draw(bell);
                Path2D clapper = new Path2D.Float();
                clapper.moveTo(7.5f, 15);
                clapper.quadTo(9, 16.8f, 10.5f, 15);
                g2.draw(clapper);
                g2.draw(new Line2D.Float(9, 2.5f, 9, 4));
            }
            case "edit" -> {
                Path2D pencil = new Path2D.Float();
                pencil.moveTo(3, 15);
                pencil.lineTo(3.8f, 11.4f);
                pencil.lineTo(12.2f, 3);
                pencil.lineTo(15, 5.8f);
                pencil.lineTo(6.6f, 14.2f);
                pencil.closePath();
                g2.draw(pencil);
                g2.draw(new Line2D.Float(10.4f, 4.8f, 13.2f, 7.6f));
            }
            case "trash" -> {
                g2.draw(new Line2D.Float(3, 5, 15, 5));
                Path2D handle = new Path2D.Float();
                handle.moveTo(7, 5);
                handle.lineTo(7, 3);
                handle.lineTo(11, 3);
                handle.lineTo(11, 5);
                g2.draw(handle);
                Path2D body = new Path2D.Float();
                body.moveTo(4.5f, 5);
                body.lineTo(5.5f, 15.5f);
                body.lineTo(12.5f, 15.5f);
                body.lineTo(13.5f, 5);
                g2.draw(body);
                g2.draw(new Line2D.Float(7.5f, 8, 7.5f, 13));
                g2.draw(new Line2D.Float(10.5f, 8, 10.5f, 13));
            }
            case "transfer" -> {
                g2.draw(new Line2D.Float(3, 6, 14.5f, 6));
                Path2D top = new Path2D.Float();
                top.moveTo(11.5f, 3);
                top.lineTo(14.5f, 6);
                top.lineTo(11.5f, 9);
                g2.draw(top);
                g2.draw(new Line2D.Float(15, 12, 3.5f, 12));
                Path2D bottom = new Path2D.Float();
                bottom.moveTo(6.5f, 9);
                bottom.lineTo(3.5f, 12);
                bottom.lineTo(6.5f, 15);
                g2.draw(bottom);
            }
            case "eye" -> {
                Path2D eye = new Path2D.Float();
                eye.moveTo(1.5f, 9);
                eye.quadTo(9, 2, 16.5f, 9);
                eye.quadTo(9, 16, 1.5f, 9);
                eye.closePath();
                g2.draw(eye);
                g2.draw(new Ellipse2D.Float(6.5f, 6.5f, 5, 5));
            }
            default -> { }
        }
        g2.dispose();
    }
}