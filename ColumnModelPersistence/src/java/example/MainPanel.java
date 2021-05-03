// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.PersistenceDelegate;
import java.beans.Statement;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

public final class MainPanel extends JPanel {
  private MainPanel() {
    super(new BorderLayout());
    String[] columnNames = {"A", "B"};
    Object[][] data = {
      {"aaa", "1234567890"}, {"bbb", "☀☁☂☃"}
    };
    JTable table = new JTable(new DefaultTableModel(data, columnNames));
    table.setAutoCreateRowSorter(true);
    table.getTableHeader().setComponentPopupMenu(new TableHeaderPopupMenu());

    JTextArea textArea = new JTextArea();

    JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    sp.setResizeWeight(.5);
    sp.setTopComponent(new JScrollPane(table));
    sp.setBottomComponent(new JScrollPane(textArea));

    JButton encodeButton = new JButton("XMLEncoder");
    encodeButton.addActionListener(e -> {
      try {
        File file = File.createTempFile("output", ".xml");
        // try (XMLEncoder xe = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)))) {
        try (XMLEncoder xe = new XMLEncoder(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
          PersistenceDelegate d = new DefaultPersistenceDelegate(new String[] {"column", "sortOrder"});
          xe.setPersistenceDelegate(RowSorter.SortKey.class, d);
          xe.writeObject(table.getRowSorter().getSortKeys());

          xe.setPersistenceDelegate(DefaultTableModel.class, new DefaultTableModelPersistenceDelegate());
          xe.writeObject(table.getModel());

          xe.setPersistenceDelegate(DefaultTableColumnModel.class, new DefaultTableColumnModelPersistenceDelegate());
          xe.writeObject(table.getColumnModel());
        }
        try (Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
          textArea.read(r, "temp");
        }
      } catch (IOException ex) {
        ex.printStackTrace();
        textArea.setText(ex.getMessage());
      }
    });

    JButton decodeButton = new JButton("XMLDecoder");
    decodeButton.addActionListener(e -> {
      String text = textArea.getText();
      if (text.isEmpty()) {
        return;
      }
      byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
      try (XMLDecoder xd = new XMLDecoder(new BufferedInputStream(new ByteArrayInputStream(bytes)))) {
        // @SuppressWarnings("unchecked")
        // List<? extends RowSorter.SortKey> keys = (List<? extends RowSorter.SortKey>) xd.readObject();
        Class<RowSorter.SortKey> clz = RowSorter.SortKey.class;
        List<? extends RowSorter.SortKey> keys = ((List<?>) xd.readObject()).stream()
            .filter(clz::isInstance)
            .map(clz::cast)
            .collect(Collectors.toList());
        DefaultTableModel model = (DefaultTableModel) xd.readObject();
        table.setModel(model);
        table.setAutoCreateRowSorter(true);
        table.getRowSorter().setSortKeys(keys);
        DefaultTableColumnModel cm = (DefaultTableColumnModel) xd.readObject();
        table.setColumnModel(cm);
      }
    });

    JButton clearButton = new JButton("clear");
    clearButton.addActionListener(e -> table.setModel(new DefaultTableModel()));

    JPanel p = new JPanel();
    p.add(encodeButton);
    p.add(decodeButton);
    p.add(clearButton);
    add(sp);
    add(p, BorderLayout.SOUTH);
    setPreferredSize(new Dimension(320, 240));
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(MainPanel::createAndShowGui);
  }

  private static void createAndShowGui() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
      ex.printStackTrace();
      Toolkit.getDefaultToolkit().beep();
    }
    JFrame frame = new JFrame("@title@");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new MainPanel());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

// http://web.archive.org/web/20090806075316/http://java.sun.com/products/jfc/tsc/articles/persistence4/
// http://www.oracle.com/technetwork/java/persistence4-140124.html
// https://ateraimemo.com/Swing/PersistenceDelegate.html
class DefaultTableModelPersistenceDelegate extends DefaultPersistenceDelegate {
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Override protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder encoder) {
    super.initialize(type, oldInstance, newInstance, encoder);
    DefaultTableModel m = (DefaultTableModel) oldInstance;
    for (int row = 0; row < m.getRowCount(); row++) {
      for (int col = 0; col < m.getColumnCount(); col++) {
        Object[] o = {m.getValueAt(row, col), row, col};
        encoder.writeStatement(new Statement(oldInstance, "setValueAt", o));
      }
    }
  }
}

class DefaultTableColumnModelPersistenceDelegate extends DefaultPersistenceDelegate {
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Override protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder encoder) {
    super.initialize(type, oldInstance, newInstance, encoder);
    DefaultTableColumnModel m = (DefaultTableColumnModel) oldInstance;
    for (int col = 0; col < m.getColumnCount(); col++) {
      Object[] o = {m.getColumn(col)};
      encoder.writeStatement(new Statement(oldInstance, "addColumn", o));
    }
  }
}

class TableHeaderPopupMenu extends JPopupMenu {
  private int index = -1;

  protected TableHeaderPopupMenu() {
    super();
    JTextField textField = new JTextField();
    textField.addAncestorListener(new FocusAncestorListener());

    add("Edit: setHeaderValue").addActionListener(e -> {
      JTableHeader header = (JTableHeader) getInvoker();
      TableColumn column = header.getColumnModel().getColumn(index);
      String name = column.getHeaderValue().toString();
      textField.setText(name);
      Component p = header.getRootPane();
      int ret = JOptionPane.showConfirmDialog(
          p, textField, "edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      if (ret == JOptionPane.OK_OPTION) {
        String str = textField.getText().trim();
        if (!str.equals(name)) {
          column.setHeaderValue(str);
          header.repaint(header.getHeaderRect(index));
        }
      }
    });
  }

  @Override public void show(Component c, int x, int y) {
    if (c instanceof JTableHeader) {
      JTableHeader header = (JTableHeader) c;
      header.setDraggedColumn(null);
      header.repaint();
      header.getTable().repaint();
      index = header.columnAtPoint(new Point(x, y));
      if (index >= 0) {
        super.show(c, x, y);
      }
    }
  }
}

class FocusAncestorListener implements AncestorListener {
  @Override public void ancestorAdded(AncestorEvent e) {
    e.getComponent().requestFocusInWindow();
  }

  @Override public void ancestorMoved(AncestorEvent e) {
    /* not needed */
  }

  @Override public void ancestorRemoved(AncestorEvent e) {
    /* not needed */
  }
}
