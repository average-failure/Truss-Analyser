package analyser.truss;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class Truss implements Serializable {

  private static final long serialVersionUID = 15249847L;

  private final List<Joint> joints;
  private final List<Member> members;
  public static final int JOINT_SIZE = 10;

  public Truss() {
    joints = new ArrayList<>();
    members = new ArrayList<>();
  }

  public Joint addJoint(Joint joint) {
    if (!joints.contains(joint)) {
      joints.add(joint);
      return joint;
    }

    return find(
      joints,
      j -> compare(j.x(), joint.x()) && compare(j.y(), joint.y()),
      joint
    );
  }

  public Joint addJoint(double x, double y) {
    return addJoint(new Joint(x, y));
  }

  public Joint addMember(Joint joint1, double x, double y) {
    final Joint joint2 = addJoint(x, y);
    addMember(joint1, joint2);
    return joint2;
  }

  public void addMember(Joint joint1, Joint joint2) {
    if (joint1.equals(joint2)) return;
    final Member member = new Member(joint1, joint2);
    if (members.contains(member)) return;
    members.add(member);
    joint1.addConnectedMember(member);
    joint2.addConnectedMember(member);
    addJoint(joint1);
    addJoint(joint2);
  }

  public long numFixedJoints() {
    return joints.stream().filter(Joint::isFixed).count();
  }

  public List<Joint> getJoints() {
    return joints;
  }

  public void deleteJoint(Joint joint) {
    joint.delete();
    joints.remove(joint);
  }

  public void deleteJoints(List<Joint> deleteList) {
    deleteList.forEach(Joint::delete);
    joints.removeAll(deleteList);
  }

  public void deleteMember(Member member) {
    members.remove(member);
  }

  public int numJoints() {
    return joints.size();
  }

  public List<Joint> getUnsolvedJoints() {
    return joints.stream().filter(Predicate.not(Joint::isSolved)).toList();
  }

  public boolean isSolved() {
    if (members.isEmpty()) return false;
    return members.stream().noneMatch(Member::isUnsolved);
  }

  public void draw(Graphics2D g2) {
    final Font font = new Font(
      "Gill Sans",
      Font.PLAIN,
      Math.max(4, 25 - members.size())
    );
    final Font bigFont = new Font(
      "Gill Sans",
      Font.PLAIN,
      Math.max(10, 20 - joints.size() / 3)
    );
    g2.setFont(font);
    members.forEach(m -> {
      g2.setColor(m.getColour());
      g2.drawLine(
        roundToInt(m.x1()),
        roundToInt(m.y1()),
        roundToInt(m.x2()),
        roundToInt(m.y2())
      );
      g2.setColor(Color.LIGHT_GRAY);
      g2.drawString(
        m.getInternalForce().toString(),
        roundToInt(
          m.cx() -
          g2.getFontMetrics().stringWidth(m.getInternalForce().toString()) /
          2d
        ),
        roundToInt(m.cy() - 1)
      );
    });
    joints.forEach(j -> {
      if (j.hasExternalForces()) {
        g2.setFont(bigFont);
        g2.setColor(Color.RED);
        drawForce(g2, j.x(), j.y(), j.getExternalForce());
        g2.setFont(font);
      }
      if (j.hasReactionForce()) {
        g2.setFont(bigFont);
        g2.setColor(Color.YELLOW);
        drawForce(g2, j.x(), j.y(), j.getReactionForce());
        g2.setFont(font);
      }
      g2.setColor(j.getColour());
      g2.fillOval(
        roundToInt(j.x()) - JOINT_SIZE / 2,
        roundToInt(j.y()) - JOINT_SIZE / 2,
        JOINT_SIZE,
        JOINT_SIZE
      );
    });
  }

  private void drawForce(Graphics2D g2, double x, double y, double force) {
    g2.drawLine(
      roundToInt(x),
      roundToInt(y),
      roundToInt(x),
      roundToInt(y + force)
    );
    g2.drawString(
      formatWithPostfix(force) + "N",
      roundToInt(x) + JOINT_SIZE,
      roundToInt(y) + g2.getFontMetrics().getHeight() + 3
    );
  }

  public List<Joint> sortJoints() {
    joints.sort((j1, j2) -> {
      if (j1.x() == j2.x()) return Double.compare(j2.y(), j1.y());
      return Double.compare(j1.x(), j2.x());
    });
    return joints;
  }

  public void resetForces() {
    members.forEach(Member::resetInternalForce);
    joints.forEach(Joint::resetReactionForce);
  }

  /* ==================== Static helper methods ==================== */

  private static String formatWithPostfix(double value) {
    final String postfix;
    final double abs = Math.abs(value);

    if (abs >= 1_000_000_000) {
      postfix = "G";
      value /= 1_000_000_000;
    } else if (abs >= 1_000_000) {
      postfix = "M";
      value /= 1_000_000;
    } else if (abs >= 1_000) {
      postfix = "k";
      value /= 1_000;
    } else postfix = "";

    return String.format("%.3f%s", value, postfix);
  }

  private static int roundToInt(double x) {
    return (int) Math.round(x);
  }

  private static <T> T find(
    Collection<T> col,
    Predicate<T> filter,
    T defaultReturn
  ) {
    return col.stream().filter(filter).findFirst().orElse(defaultReturn);
  }

  private static boolean compare(double d1, double d2) {
    final double threshold = 0.01;
    return Math.abs(d1 - d2) < threshold;
  }

  /* ==================== Overrides ==================== */

  @Override
  public String toString() {
    return String.format("Truss [joints=%s, members=%s]", joints, members);
  }

  /* ==================== Truss building methods ==================== */

  public enum Type {
    WARREN,
    PRATT,
    HOWE,
  }

  public static Truss build(
    Type type,
    int segments,
    double width,
    double height
  ) {
    if (segments < 3) segments = 3;
    return switch (type) {
      case WARREN -> buildWarren(segments, width, height);
      case PRATT -> buildPratt(segments, width, height);
      case HOWE -> buildHowe(segments, width, height);
      default -> new Truss();
    };
  }

  private static Truss buildWarren(int segments, double width, double height) {
    final double sin = Math.sin(Math.toRadians(60)) * width;
    if (height != sin) height = sin;

    final Truss truss = new Truss();

    final int startX = 30;
    final int startY = Math.max(300, 550 - segments * 10);

    // Add bottom joints and members
    final List<Joint> bottom = new ArrayList<>();
    bottom.add(truss.addJoint(startX, startY));
    for (int i = 1; i < segments; i++) {
      bottom.add(
        truss.addMember(bottom.get(i - 1), startX + i * width, startY)
      );
    }

    // Add top joints and right diagonal members
    final List<Joint> top = new ArrayList<>();
    for (int i = 0; i < segments - 1; i++) {
      top.add(
        truss.addMember(
          bottom.get(i),
          startX + i * width + width / 2,
          startY - height
        )
      );
    }

    // Add left diagonal members
    for (int i = 1; i < segments; i++) {
      truss.addMember(bottom.get(i), top.get(i - 1));
    }

    // Add top members
    for (int i = 1; i < top.size(); i++) {
      truss.addMember(top.get(i - 1), top.get(i));
    }

    // Fix bottom joints at ends
    bottom.get(0).setFixed(true);
    bottom.get(bottom.size() - 1).setFixed(true);

    return truss;
  }

  private static Truss buildPratt(int segments, double width, double height) {
    final Truss truss = new Truss();

    final int startX = 30;
    final int startY = Math.max(300, 550 - segments * 10);

    // Add bottom joints and members
    final List<Joint> bottom = new ArrayList<>();
    bottom.add(truss.addJoint(startX, startY));
    for (int i = 1; i < segments; i++) {
      bottom.add(
        truss.addMember(bottom.get(i - 1), startX + i * width, startY)
      );
    }

    // Add top joints and vertical members
    final List<Joint> top = new ArrayList<>();
    for (int i = 1; i < segments - 1; i++) {
      top.add(
        truss.addMember(bottom.get(i), bottom.get(i).x(), startY - height)
      );
    }

    final int split = segments / 2;

    // Add left diagonal members
    for (int i = 2; i <= split; i++) {
      truss.addMember(bottom.get(i), top.get(i - 2));
    }

    // Add right diagonal members
    for (int i = split; i < segments - 2; i++) {
      truss.addMember(bottom.get(i), top.get(i));
    }

    // Add top members
    for (int i = 1; i < top.size(); i++) {
      truss.addMember(top.get(i - 1), top.get(i));
    }

    // Add members at ends
    truss.addMember(bottom.get(0), top.get(0));
    truss.addMember(bottom.get(bottom.size() - 1), top.get(top.size() - 1));

    // Fix bottom joints at ends
    bottom.get(0).setFixed(true);
    bottom.get(bottom.size() - 1).setFixed(true);

    return truss;
  }

  private static Truss buildHowe(int segments, double width, double height) {
    final Truss truss = new Truss();

    final int startX = 30;
    final int startY = Math.max(300, 550 - segments * 10);

    // Add bottom joints and members
    final List<Joint> bottom = new ArrayList<>();
    bottom.add(truss.addJoint(startX, startY));
    for (int i = 1; i < segments; i++) {
      bottom.add(
        truss.addMember(bottom.get(i - 1), startX + i * width, startY)
      );
    }

    // Add top joints and vertical members
    final List<Joint> top = new ArrayList<>();
    for (int i = 1; i < segments - 1; i++) {
      top.add(
        truss.addMember(bottom.get(i), bottom.get(i).x(), startY - height)
      );
    }

    final int split = top.size() / 2;

    // Add left diagonal members
    for (int i = 0; i <= split; i++) {
      truss.addMember(bottom.get(i), top.get(i));
    }

    // Add right diagonal members
    for (int i = split; i < top.size(); i++) {
      truss.addMember(bottom.get(i + 2), top.get(i));
    }

    // Add top members
    for (int i = 1; i < top.size(); i++) {
      truss.addMember(top.get(i - 1), top.get(i));
    }

    // Fix bottom joints at ends
    bottom.get(0).setFixed(true);
    bottom.get(bottom.size() - 1).setFixed(true);

    return truss;
  }
}
