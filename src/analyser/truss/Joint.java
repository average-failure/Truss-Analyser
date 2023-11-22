package analyser.truss;

import analyser.App;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Joint implements Serializable {

  private static final long serialVersionUID = 2497505955L;

  public static final Color BASE_COLOUR = Color.BLUE;
  public static final Color FIXED_COLOUR = Color.CYAN;
  public static final Color HOVER_COLOUR = Color.RED;
  public static final Color DRAG_COLOUR = Color.GREEN;

  private double x;
  private double y;
  private final Ellipse2D.Double bounds;
  private final List<Member> connectedMembers = new ArrayList<>();
  private double externalForce = 0;
  private double reactionForce = 0;
  private boolean fixed = false;
  private Color colour;

  public Joint(double x, double y) {
    this.x = x;
    this.y = y;
    resetColour();
    bounds =
      new Ellipse2D.Double(
        x - Truss.JOINT_SIZE / 2d,
        y - Truss.JOINT_SIZE / 2d,
        Truss.JOINT_SIZE,
        Truss.JOINT_SIZE
      );
  }

  public Joint(double x, double y, double... externalForces) {
    this(x, y);
    for (double force : externalForces) externalForce += force;
  }

  public boolean isFixed() {
    return fixed;
  }

  public void setFixed(boolean isFixed) {
    this.fixed = isFixed;
    resetColour();
  }

  public double x() {
    return x;
  }

  public double y() {
    return y;
  }

  public Rectangle getBounds() {
    return bounds.getBounds();
  }

  public boolean contains(double x, double y) {
    return bounds.contains(x, y);
  }

  public boolean isHovered() {
    return colour == HOVER_COLOUR;
  }

  public Color getColour() {
    return colour;
  }

  public void setColour(Color colour) {
    this.colour = colour;
  }

  public void resetColour() {
    colour = fixed ? FIXED_COLOUR : BASE_COLOUR;
  }

  public boolean moveTo(double x, double y) {
    if (this.x == x && this.y == y) return false;
    this.x = x;
    this.y = y;
    bounds.setFrame(
      x - Truss.JOINT_SIZE / 2d,
      y - Truss.JOINT_SIZE / 2d,
      Truss.JOINT_SIZE,
      Truss.JOINT_SIZE
    );
    return true;
  }

  public void moveTo(double[] pos) {
    if (pos == null || pos.length != 2) return;
    moveTo(pos[0], pos[1]);
  }

  public void delete() {
    connectedMembers.forEach(App.getTruss()::deleteMember);
    connectedMembers.clear();
  }

  public void addConnectedMember(Member connectedMember) {
    if (connectedMembers.contains(connectedMember)) return;
    connectedMembers.add(connectedMember);
  }

  public void resetReactionForce() {
    reactionForce = 0;
  }

  public void addExternalForce(double y) {
    externalForce += y;
  }

  public void addReactionForce(double y) {
    reactionForce += y;
  }

  public boolean hasExternalForces() {
    return externalForce != 0;
  }

  public boolean hasReactionForce() {
    return reactionForce != 0;
  }

  public List<Member> getConnectedMembers() {
    return connectedMembers;
  }

  public List<Joint> getConnectedJoints() {
    return connectedMembers.stream().map(m -> m.getOtherJoint(this)).toList();
  }

  public boolean isSolved() {
    if (connectedMembers.isEmpty()) return false;
    return connectedMembers.stream().noneMatch(Member::isUnsolved);
  }

  public List<Member> getUnsolved() {
    return connectedMembers.stream().filter(Member::isUnsolved).toList();
  }

  public List<Member> getSolved() {
    return connectedMembers
      .stream()
      .filter(Predicate.not(Member::isUnsolved))
      .toList();
  }

  public double getExternalForce() {
    return externalForce;
  }

  public double getReactionForce() {
    return reactionForce;
  }

  public double sumForces() {
    return externalForce + reactionForce;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(x);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Joint other = (Joint) obj;
    if (
      Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)
    ) return false;
    return Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y);
  }

  @Override
  public String toString() {
    return String.format(
      "Joint [x=%s, y=%s, externalForce=%s, reactionForce=%s, isFixed=%s, isSolved=%s]",
      x,
      y,
      externalForce,
      reactionForce,
      fixed,
      isSolved()
    );
  }
}
