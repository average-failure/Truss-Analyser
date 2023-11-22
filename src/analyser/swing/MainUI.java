package analyser.swing;

import analyser.App;
import analyser.core.Analyser;
import analyser.truss.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.event.PPanEventHandler;
import org.piccolo2d.event.PZoomEventHandler;
import org.piccolo2d.extras.pswing.PSwingCanvas;
import org.piccolo2d.util.PPaintContext;

public class MainUI extends JFrame {

  private class MouseHandler extends PBasicInputEventHandler {

    private record Point(double x, double y) {
      public double distanceToSq(double x, double y) {
        final double dx = x - this.x;
        final double dy = y - this.y;
        return dx * dx + dy * dy;
      }
    }

    private final List<Joint> selectedJoints = new ArrayList<>();

    private double sx;
    private double sy;

    @Override
    public void mouseMoved(PInputEvent e) {
      final Point2D point = e.getPosition();
      final double x = point.getX();
      final double y = point.getY();
      for (Joint joint : App.getTruss().getJoints()) {
        if (selectedJoints.contains(joint)) continue;
        final boolean contains = joint.contains(x, y);
        if (contains && !joint.isHovered()) {
          joint.setColour(Joint.HOVER_COLOUR);
          contentPanel.repaint();
        } else if (!contains && joint.isHovered()) {
          joint.resetColour();
          contentPanel.repaint();
        }
      }
    }

    @Override
    public void mousePressed(PInputEvent e) {
      if (e.isRightMouseButton()) return;

      final Point2D point = e.getPosition();
      final double y = point.getY();
      final double x = point.getX();

      if (mode == Mode.MULTI_SELECT) {
        sx = x;
        sy = y;
        return;
      } else selectJoint(x, y);

      if (mode == Mode.NEW_JOINT) {
        addJoint(x, y);
      } else if (mode == Mode.DELETE_JOINT) {
        final Joint joint = findJoint(x, y);
        if (joint == null) return;
        App.getTruss().deleteJoint(joint);
      } else if (mode == Mode.FIX_JOINT) {
        if (!fixJoint()) return;
      } else if (mode == Mode.ADD_FORCE) {
        final Joint joint = findJoint(x, y);
        if (joint == null) return;
        joint.addExternalForce(
          new AddForceDialog(MainUI.this, x, y).getDoubleValue()
        );
      }

      analyseTruss();
    }

    private void addJoint(double x, double y) {
      App.getTruss().addJoint(x, y);
      contentPanel.repaint();
    }

    private void addJoint(double[] pos) {
      App.getTruss().addJoint(pos[0], pos[1]);
      contentPanel.repaint();
    }

    /**
     * @return A boolean indicating whether the function returned early or not
     */
    private boolean fixJoint() {
      if (selectedJoints.isEmpty()) return false;
      final Joint selectedJoint = selectedJoints.get(0);
      if (
        selectedJoint == null ||
        (!selectedJoint.isFixed() && App.getTruss().numFixedJoints() >= 2)
      ) return false;
      selectedJoint.setFixed(!selectedJoint.isFixed());
      return true;
    }

    private void selectJoint(double x, double y) {
      final Joint joint = findJoint(x, y);
      if (joint == null) return;
      selectedJoints.add(joint);
      joint.setColour(Joint.DRAG_COLOUR);
      contentPanel.repaint();
    }

    private void selectJoints(double x1, double y1, double x2, double y2) {
      final Rectangle2D.Double selection = wrapRect(x1, y1, x2, y2);
      for (Joint joint : App.getTruss().getJoints()) {
        if (
          selection.contains(joint.x(), joint.y()) &&
          !selectedJoints.contains(joint)
        ) {
          selectedJoints.add(joint);
          joint.setColour(Joint.DRAG_COLOUR);
        } else if (
          !selection.contains(joint.x(), joint.y()) &&
          selectedJoints.contains(joint)
        ) {
          selectedJoints.remove(joint);
          joint.resetColour();
          contentPanel.repaint();
        }
      }
    }

    private Joint findJoint(double x, double y) {
      for (Joint joint : App.getTruss().getJoints()) {
        if (joint.contains(x, y)) return joint;
      }
      return null;
    }

    private Rectangle2D.Double wrapRect(
      double x1,
      double y1,
      double x2,
      double y2
    ) {
      double x = x1;
      double y = y1;
      double w = x2 - x1;
      double h = y2 - y1;
      if (w < 0) {
        w = -w;
        x -= w;
      }
      if (h < 0) {
        h = -h;
        y -= h;
      }
      return new Rectangle2D.Double(x, y, w, h);
    }

    @Override
    public void mouseDragged(PInputEvent e) {
      mouseMoved(e);

      final Point2D point = e.getPosition();
      final double x = point.getX();
      final double y = point.getY();

      if (mode == Mode.MULTI_SELECT) {
        selectJoints(sx, sy, x, y);
        draw = g -> g.draw(wrapRect(sx, sy, x, y));
        contentPanel.repaint();
      }

      if (selectedJoints.isEmpty()) return;

      if (mode == Mode.NEW_MEMBER) {
        final Joint selectedJoint = selectedJoints.get(0);
        draw =
          g ->
            g.drawLine(
              (int) selectedJoint.x(),
              (int) selectedJoint.y(),
              (int) x,
              (int) y
            );
        contentPanel.repaint();
      } else if (mode == Mode.MOVE_JOINT) moveJoint(x, y, e);
    }

    private void moveJoint(double x, double y, PInputEvent e) {
      if (selectedJoints.isEmpty()) return;
      final Joint joint = selectedJoints.get(0);

      if (
        (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) ==
        InputEvent.SHIFT_DOWN_MASK
      ) {
        if (selectedJoints.get(0).moveTo(x, y)) analyseTruss();
        return;
      }

      final Point snappedPos = snapLength(joint, x, y, 300);
      if (snappedPos == null) return;
      if (
        selectedJoints.get(0).moveTo(snappedPos.x(), snappedPos.y())
      ) analyseTruss();
    }

    private Point snapLength(Joint joint, double x, double y, double length) {
      final List<Joint> connectedJoints = joint.getConnectedJoints();
      if (connectedJoints.isEmpty()) return new Point(x, y);
      final int len = connectedJoints.size();
      if (len == 1) {
        return snapLength(
          connectedJoints.get(0).x(),
          connectedJoints.get(0).y(),
          x,
          y,
          length
        );
      }

      final List<Point> intersections = new ArrayList<>();

      for (int i = 0; i < len; i++) {
        for (int j = 0; j < len; j++) {
          if (i == j) continue;
          intersections.addAll(
            getIntersections(
              connectedJoints.get(i),
              connectedJoints.get(j),
              length
            )
          );
        }
      }

      final List<Point> intersectsAll = new ArrayList<>();
      intersections
        .stream()
        .collect(Collectors.groupingBy(i -> i))
        .forEach((k, v) -> {
          if (v.size() == len) intersectsAll.add(k);
        });

      if (intersectsAll.isEmpty()) return new Point(x, y);
      if (intersectsAll.size() == 1) return intersectsAll.get(0);

      Point closest = intersectsAll.get(0);
      double closestDistance = closest.distanceToSq(x, y);
      for (int i = 1; i < intersectsAll.size(); i++) {
        final double d = intersectsAll.get(i).distanceToSq(x, y);
        if (d < closestDistance) {
          closest = intersectsAll.get(i);
          closestDistance = d;
        }
      }

      return closest;
    }

    private List<Point> getIntersections(Joint j1, Joint j2, double length) {
      return getIntersections(j1.x(), j1.y(), j2.x(), j2.y(), length);
    }

    private List<Point> getIntersections(
      double x1,
      double y1,
      double x2,
      double y2,
      double length
    ) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      final double d = Math.sqrt(dx * dx + dy * dy);

      if (d > length + length || d <= 0) return List.of();

      dx /= d;
      dy /= d;

      final double a = (d * d) / (2 * d);
      final double px = x1 + a * dx;
      final double py = y1 + a * dy;
      final double h = Math.sqrt(length * length - a * a);

      return List.of(
        new Point(px + h * dy, py - h * dx),
        new Point(px - h * dy, py + h * dx)
      );
    }

    private Point snapLength(
      double x1,
      double y1,
      double x2,
      double y2,
      double length
    ) {
      final double dx = x2 - x1;
      final double dy = y2 - y1;
      final double hyp = Math.sqrt(dx * dx + dy * dy);
      return new Point(x1 + length * (dx / hyp), y1 + length * (dy / hyp));
    }

    @Override
    public void mouseReleased(PInputEvent e) {
      draw = null;
      if (!selectedJoints.isEmpty()) {
        final Point2D point = e.getPosition();
        final double x = point.getX();
        final double y = point.getY();
        if (mode == Mode.NEW_MEMBER) {
          final Joint joint = findJoint(x, y);
          App
            .getTruss()
            .addMember(
              selectedJoints.get(0),
              joint != null ? joint : new Joint(x, y)
            );
          analyseTruss();
        }
        if (mode != Mode.MULTI_SELECT) resetSelected();
      }
      contentPanel.repaint();
    }

    @Override
    public void mouseClicked(PInputEvent e) {
      if (e.isRightMouseButton()) {
        final Point2D point = e.getPosition();
        final double x = point.getX();
        final double y = point.getY();
        selectJoint(x, y);
        if (findJoint(x, y) != null) {
          menuItems.forEach(menu::add);
        } else menuItems.forEach(menu::remove);
        menu.show(MainUI.this, (int) x, (int) y);
      }
    }

    private void resetSelected() {
      if (selectedJoints.isEmpty()) return;
      selectedJoints.forEach(Joint::resetColour);
      selectedJoints.clear();
    }
  }

  private void analyseTruss() {
    if (App.getTruss() == null) return;
    trussValid = Analyser.analyseTruss(App.getTruss());
    contentPanel.repaint();
  }

  private enum Mode {
    NEW_JOINT,
    MOVE_JOINT,
    DELETE_JOINT,
    FIX_JOINT,
    NEW_MEMBER,
    ADD_FORCE,
    MULTI_SELECT,
  }

  private Mode mode = Mode.ADD_FORCE;
  private final PNode contentPanel;
  private final JPopupMenu menu;
  private final List<JMenuItem> menuItems;
  private boolean trussValid;
  private transient Consumer<Graphics2D> draw;

  public MainUI() {
    final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
    size.height *= 0.8;
    size.width *= 0.9;
    setSize(size);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    final JFrame controlsFrame = new JFrame("Controls");

    final JComboBox<String> modeBox = new JComboBox<>(
      toNormalString(Mode.values())
    );
    modeBox.setSelectedIndex(5);
    controlsFrame.add(modeBox);
    modeBox.addItemListener(this::setMode);

    final JButton saveButton = new JButton("Save Truss");
    controlsFrame.add(saveButton);
    saveButton.addActionListener(this::saveTruss);

    final JButton loadButton = new JButton("Load Truss");
    controlsFrame.add(loadButton);
    loadButton.addActionListener(this::loadTruss);

    final JButton resetButton = new JButton("Reset Truss");
    controlsFrame.add(resetButton);
    resetButton.addActionListener(e -> App.resetTruss());

    final JComboBox<String> genTrussBox = new JComboBox<>(
      toNormalString(Truss.Type.values())
    );
    controlsFrame.add(genTrussBox);

    final JButton genTrussButton = new JButton("Generate Truss");
    controlsFrame.add(genTrussButton);
    genTrussButton.addActionListener(e -> {
      App.loadTruss(
        Truss.build(
          Truss.Type.valueOf(toEnumString(genTrussBox.getSelectedItem())),
          new NumberDialog(this, genTrussButton.getX(), genTrussButton.getY())
            .getIntValue(),
          new NumberDialog(this, genTrussButton.getX(), genTrussButton.getY())
            .getDoubleValue(),
          new NumberDialog(this, genTrussButton.getX(), genTrussButton.getY())
            .getDoubleValue()
        )
      );
      analyseTruss();
    });

    contentPanel =
      new PNode() {
        @Override
        protected void paint(PPaintContext ctx) {
          super.paint(ctx);
          final Graphics2D g2 = ctx.getGraphics();
          g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
          );
          App.getTruss().draw(g2);
          if (!trussValid) {
            g2.setFont(new Font("Gill Sans", Font.PLAIN, 20));
            g2.setColor(Color.ORANGE);
            g2.drawString("Truss not valid", 10, 30);
          }
          if (draw != null) {
            g2.setColor(Color.MAGENTA);
            draw.accept(g2);
          }
        }
      };
    contentPanel.setBounds(0, 0, size.getWidth(), size.getHeight());
    contentPanel.setPaint(new Color(25, 25, 25));
    final PSwingCanvas canvas = new PSwingCanvas();
    canvas.getLayer().addChild(contentPanel);

    final JCheckBox checkbox = new JCheckBox("Controls");
    checkbox.setSelected(true);
    controlsFrame.add(checkbox);
    checkbox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        canvas.setPanEventHandler(new PPanEventHandler());
        canvas.setZoomEventHandler(new PZoomEventHandler());
      } else {
        canvas.setPanEventHandler(null);
        canvas.setZoomEventHandler(null);
      }
    });

    controlsFrame.setLayout(new FlowLayout());
    controlsFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    controlsFrame.pack();
    controlsFrame.setVisible(true);

    getContentPane().add(canvas, BorderLayout.CENTER);

    final MouseHandler handler = new MouseHandler();
    contentPanel.addInputEventListener(handler);

    analyseTruss();

    class Point {

      int x;
      int y;

      void set(int x, int y) {
        this.x = x;
        this.y = y;
      }
    }

    Point menuPos = new Point();

    menu =
      new JPopupMenu("Menu") {
        @Override
        public void show(Component invoker, int x, int y) {
          super.show(invoker, x, y);
          menuPos.set(x, y);
        }
      };
    menu
      .add(new JMenuItem("New Joint"))
      .addActionListener(e ->
        handler.addJoint(
          new PositionDialog(this, menuPos.x, menuPos.y).getDoubleValues()
        )
      );

    menuItems = new ArrayList<>();

    final JMenuItem moveJoint = new JMenuItem("Move Joint");
    moveJoint.addActionListener(e -> {
      if (handler.selectedJoints.isEmpty()) return;
      final double[] movement = new PositionDialog(this, menuPos.x, menuPos.y)
        .getDoubleValues();
      handler.selectedJoints.forEach(j ->
        j.moveTo(j.x() + movement[0], j.y() + movement[1])
      );
      handler.resetSelected();
      analyseTruss();
    });
    menuItems.add(moveJoint);

    final JMenuItem fixJoint = new JMenuItem("Fix Joint");
    fixJoint.addActionListener(e -> {
      handler.fixJoint();
      handler.resetSelected();
      analyseTruss();
    });
    menuItems.add(fixJoint);

    final JMenuItem deleteJoint = new JMenuItem("Delete Joint");
    deleteJoint.addActionListener(e -> {
      if (handler.selectedJoints.isEmpty()) return;
      App.getTruss().deleteJoints(handler.selectedJoints);
      handler.resetSelected();
      analyseTruss();
    });
    menuItems.add(deleteJoint);

    final JMenuItem addForce = new JMenuItem("Add External Force");
    addForce.addActionListener(e -> {
      if (handler.selectedJoints.isEmpty()) return;
      final double force = new AddForceDialog(this, menuPos.x, menuPos.y)
        .getDoubleValue();
      handler.selectedJoints.forEach(j -> j.addExternalForce(force));
      handler.resetSelected();
      analyseTruss();
    });
    menuItems.add(addForce);
  }

  private void saveTruss(ActionEvent e) {
    final JFileChooser chooser = new JFileChooser();
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      try (
        ObjectOutputStream oos = new ObjectOutputStream(
          new FileOutputStream(chooser.getSelectedFile())
        )
      ) {
        oos.writeObject(App.getTruss());
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  private void loadTruss(ActionEvent e) {
    final JFileChooser chooser = new JFileChooser();
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      try (
        ObjectInputStream oos = new ObjectInputStream(
          new FileInputStream(chooser.getSelectedFile())
        )
      ) {
        App.loadTruss((Truss) oos.readObject());
        analyseTruss();
      } catch (IOException | ClassNotFoundException ex) {
        ex.printStackTrace();
      }
    }
  }

  private void setMode(ItemEvent e) {
    mode = Mode.valueOf(toEnumString(e.getItem()));
  }

  private String toEnumString(Object object) {
    return String
      .valueOf(object)
      .toUpperCase(Locale.ROOT)
      .trim()
      .replace(" ", "_");
  }

  private <T extends Enum<T>> String[] toNormalString(T[] values) {
    final String[] strings = new String[values.length];
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      final String[] strArr = values[i].toString().split("_");
      sb.setLength(0);
      for (String str : strArr) {
        sb.append(
          str.substring(0, 1).toUpperCase(Locale.ROOT) +
          str.substring(1).toLowerCase(Locale.ROOT) +
          " "
        );
      }
      strings[i] = sb.toString().trim();
    }
    return strings;
  }
}
