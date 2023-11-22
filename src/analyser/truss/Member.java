package analyser.truss;

import java.awt.Color;
import java.io.Serializable;

public class Member implements Serializable {

  private static final long serialVersionUID = 523485389L;

  private final Joint joint1;
  private final Joint joint2;
  private Force internalForce;
  private boolean unsolved = true;

  public Member(Joint joint1, Joint joint2) {
    this.joint1 = joint1;
    this.joint2 = joint2;

    resetInternalForce();
  }

  public Joint getOtherJoint(Joint joint) {
    if (joint == joint1) return joint2; else if (joint == joint2) return joint1;
    throw new IllegalArgumentException("Joint not connected to member");
  }

  public double x1() {
    return joint1.x();
  }

  public double y1() {
    return joint1.y();
  }

  public double x2() {
    return joint2.x();
  }

  public double y2() {
    return joint2.y();
  }

  public double cx() {
    return (x1() + x2()) / 2;
  }

  public double cy() {
    return (y1() + y2()) / 2;
  }

  public double getAngle() {
    double angle = Math.atan2(y1() - y2(), x1() - x2());
    if (x2() >= x1()) angle = Math.atan2(y2() - y1(), x2() - x1());
    while (angle < 0) angle += Math.PI * 2;
    while (angle >= Math.PI * 2) angle -= Math.PI * 2;
    return angle;
  }

  public void resetInternalForce() {
    internalForce = Force.byMagnitude(0, getAngle());
    unsolved = true;
  }

  /**
   * @return the internalForce
   */
  public Force getInternalForce() {
    return internalForce;
  }

  public void setInternalForce(double magnitude) {
    internalForce = Force.byMagnitude(magnitude, getAngle());
    unsolved = false;
  }

  public Color getColour() {
    return Color.getHSBColor(
      (float) internalForce.magnitude() / 800,
      0.9f,
      0.75f
    );
  }

  public boolean isUnsolved() {
    return unsolved;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((joint1 == null) ? 0 : joint1.hashCode());
    result = prime * result + ((joint2 == null) ? 0 : joint2.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Member other = (Member) obj;
    return (
      (joint1 == other.joint1 && joint2 == other.joint2) ||
      (joint1 == other.joint2 && joint2 == other.joint1)
    );
  }

  @Override
  public String toString() {
    return String.format(
      "Member [joint1=%s, joint2=%s, internalForce=%s, unsolved=%s]",
      joint1,
      joint2,
      internalForce,
      unsolved
    );
  }
}
