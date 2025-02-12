// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.*;

public final class MainPanel extends JPanel {
  private static final String TXT = "***********************";
  private static final int LINE_WIDTH = 1;
  private static final int BI_GAP = 2;

  private MainPanel() {
    super(new GridLayout(0, 1));
    setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 0));
    add(makeBreadcrumbList(0, Color.PINK, Arrays.asList("overlap1:", "0px", TXT)));
    add(makeBreadcrumbList(5, Color.CYAN, Arrays.asList("overlap2:", "5px", TXT)));
    add(makeBreadcrumbList(9, Color.ORANGE, Arrays.asList("overlap3:", "9px", TXT)));
    setPreferredSize(new Dimension(320, 240));
  }

  private static AbstractButton makeButton(String title, Color color, boolean first) {
    // https://java-swing-tips.blogspot.com/2008/11/rounded-corner-jbutton.html
    AbstractButton b = new JToggleButton(title) {
      private final transient ArrowToggleButtonCellIcon icon = new ArrowToggleButtonCellIcon();
      @Override public boolean contains(int x, int y) {
        Shape s = icon.getShape();
        return Objects.nonNull(s) && s.contains(x, y);
      }

      @Override public Dimension getPreferredSize() {
        return new Dimension(icon.getIconWidth(), icon.getIconHeight());
      }

      @Override protected void paintComponent(Graphics g) {
        icon.paintIcon(this, g, 0, 0);
        super.paintComponent(g);
      }
    };
    b.setIcon(new Icon() {
      @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(Color.GRAY);
        g.drawOval(x, y, getIconWidth(), getIconHeight());
      }

      @Override public int getIconWidth() {
        return 12;
      }

      @Override public int getIconHeight() {
        return 12;
      }
    });
    b.setContentAreaFilled(false);
    int th = ArrowToggleButtonCellIcon.TH;
    int left = LINE_WIDTH + BI_GAP + (first ? 0 : th);
    b.setBorder(BorderFactory.createEmptyBorder(0, left, 0, th));
    b.setHorizontalAlignment(SwingConstants.LEFT);
    b.setFocusPainted(false);
    b.setOpaque(false);
    b.setBackground(color);
    return b;
  }

  private static Container makeContainer(int overlap) {
    // https://java-swing-tips.blogspot.com/2013/12/breadcrumb-navigation-with-jradiobutton.html
    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING, -overlap, 0)) {
      @Override public boolean isOptimizedDrawingEnabled() {
        return false;
      }
    };
    p.setBorder(BorderFactory.createEmptyBorder(0, overlap, 0, 0));
    p.setOpaque(false);
    return p;
  }

  private static Component makeBreadcrumbList(int overlap, Color color, List<String> list) {
    Container p = makeContainer(overlap + LINE_WIDTH);
    ButtonGroup bg = new ButtonGroup();
    boolean f = true;
    for (String title : list) {
      AbstractButton b = makeButton(title, color, f);
      p.add(b);
      bg.add(b);
      f = false;
    }
    return p;
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(MainPanel::createAndShowGui);
  }

  private static void createAndShowGui() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (UnsupportedLookAndFeelException ignored) {
      Toolkit.getDefaultToolkit().beep();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
      ex.printStackTrace();
      return;
    }
    JFrame frame = new JFrame("@title@");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new MainPanel());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

// https://ateraimemo.com/Swing/ToggleButtonBar.html
// https://java-swing-tips.blogspot.com/2012/11/make-togglebuttonbar-with-jradiobuttons.html
class ArrowToggleButtonCellIcon implements Icon {
  public static final int TH = 10; // The height of a triangle
  private static final int HEIGHT = TH * 2 + 1;
  private static final int WIDTH = 100;
  private Shape shape;

  public Shape getShape() {
    return shape;
  }

  protected Shape makeShape(Container parent, Component c, int x, int y) {
    int w = c.getWidth() - 1;
    int h = c.getHeight() - 1;
    double h2 = Math.round(h * .5);
    double w2 = TH;
    Path2D p = new Path2D.Double();
    p.moveTo(0d, 0d);
    p.lineTo(w - w2, 0d);
    p.lineTo(w, h2);
    p.lineTo(w - w2, h);
    p.lineTo(0d, h);
    if (!Objects.equals(c, parent.getComponent(0))) {
      p.lineTo(w2, h2);
    }
    p.closePath();
    return AffineTransform.getTranslateInstance(x, y).createTransformedShape(p);
  }

  @Override public void paintIcon(Component c, Graphics g, int x, int y) {
    Container parent = c.getParent();
    if (Objects.isNull(parent)) {
      return;
    }
    shape = makeShape(parent, c, x, y);

    Color bgc = parent.getBackground();
    Color borderColor = Color.GRAY.brighter();
    if (c instanceof AbstractButton) {
      ButtonModel m = ((AbstractButton) c).getModel();
      if (m.isSelected() || m.isRollover()) {
        bgc = c.getBackground();
        borderColor = Color.GRAY;
      }
    }
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setPaint(bgc);
    g2.fill(shape);
    g2.setPaint(borderColor);
    g2.draw(shape);
    g2.dispose();
  }

  @Override public int getIconWidth() {
    return WIDTH;
  }

  @Override public int getIconHeight() {
    return HEIGHT;
  }
}
