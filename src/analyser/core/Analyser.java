package analyser.core;

import analyser.truss.*;
import java.util.ArrayList;
import java.util.List;

public class Analyser {

  private Analyser() {}

  private static boolean analyseJoint(Joint joint) {
    if (joint.isSolved()) return true;
    final List<Member> unsolved = joint.getUnsolved();
    if (unsolved.size() > 3) return false;

    double fx = 0;
    double fy = joint.sumForces();

    for (Member member : joint.getSolved()) {
      fx += member.getInternalForce().x();
      fy += member.getInternalForce().y();
    }

    if (unsolved.size() == 1) {
      unsolved.get(0).setInternalForce(Math.sqrt(fx * fx + fy * fy));
    } else if (unsolved.size() == 2) {
      if (unsolved.stream().anyMatch(Analyser::isHorizontal)) {
        analyse2H(unsolved, fx, fy);
      } else if (unsolved.stream().anyMatch(Analyser::isVertical)) {
        analyse2V(unsolved, fx, fy);
      }
    }

    return joint.isSolved();
  }

  private static void analyse2H(List<Member> unsolved, double fx, double fy) {
    Member horizontal;
    Member other;
    if (isHorizontal(unsolved.get(0))) {
      horizontal = unsolved.get(0);
      other = unsolved.get(1);
    } else if (isHorizontal(unsolved.get(1))) {
      horizontal = unsolved.get(1);
      other = unsolved.get(0);
    } else return;
    other.setInternalForce(fy / Math.sin(other.getAngle()));
    horizontal.setInternalForce(fx - other.getInternalForce().x());
  }

  private static void analyse2V(List<Member> unsolved, double fx, double fy) {
    Member vertical;
    Member other;
    if (isVertical(unsolved.get(0))) {
      vertical = unsolved.get(0);
      other = unsolved.get(1);
    } else if (isVertical(unsolved.get(1))) {
      vertical = unsolved.get(1);
      other = unsolved.get(0);
    } else return;
    other.setInternalForce(fx / Math.cos(other.getAngle()));
    vertical.setInternalForce(fy - other.getInternalForce().y());
  }

  private static boolean isHorizontal(Member member) {
    final double angle = member.getAngle();
    return angle == Math.PI || angle == 0 || angle == Math.PI * 2;
  }

  private static boolean isVertical(Member member) {
    final double angle = member.getAngle();
    return angle == Math.PI / 2 || angle == Math.PI * 1.5;
  }

  public static boolean analyseTruss(Truss truss) {
    if (truss == null) {
      throw new IllegalArgumentException("Truss must not be null");
    } else if (truss.numJoints() <= 1) return false;

    truss.resetForces();
    final List<Joint> joints = findReactions(truss.sortJoints());
    int i = 0;
    while (i < joints.size()) {
      final boolean solved = analyseJoint(joints.get(i));
      if (!solved) analyseJoint(joints.get(i + 1)); else i++;
    }

    return truss.isSolved();
  }

  private static List<Joint> findReactions(List<Joint> joints) {
    if (
      !joints.get(0).isFixed() || !joints.get(joints.size() - 1).isFixed()
    ) return new ArrayList<>(0);

    final double x = joints.get(0).x();

    double moment = 0;
    for (Joint joint : joints) {
      if (joint.hasExternalForces()) {
        moment -= joint.getExternalForce() * (joint.x() - x);
      }
    }

    final Joint lastJoint = joints.get(joints.size() - 1);
    lastJoint.addReactionForce(moment / (lastJoint.x() - x));

    double force = 0;
    for (Joint joint : joints) force -= joint.sumForces();
    joints.get(0).addReactionForce(force);

    return joints;
  }
}
