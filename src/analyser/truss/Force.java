package analyser.truss;

import java.io.Serializable;

public record Force(double x, double y, double magnitude, double angle)
  implements Serializable {
  private static final long serialVersionUID = 9237524L;

  public static Force byXY(double x, double y) {
    return new Force(
      check(x),
      check(y),
      check(Math.sqrt(x * x + y * y)),
      check(Math.atan2(y, x))
    );
  }

  private static double check(double v) {
    if (Double.isFinite(v)) return v;
    return 0;
  }

  public static Force byMagnitude(double magnitude, double angle) {
    return new Force(
      check(magnitude * Math.cos(angle)),
      check(magnitude * Math.sin(angle)),
      check(magnitude),
      check(angle)
    );
  }

  @Override
  public String toString() {
    final boolean byXY = false;
    if (byXY) {
      return String.format("Force [x=%.3f, y=%.3f]", x, y);
    } else return String.format(
      "%sN (%s)",
      formatWithPostfix(Math.abs(magnitude)),
      magnitude > 0 ? "C" : "T"
    );
  }

  private String formatWithPostfix(double value) {
    final String postfix;

    if (value >= 1_000_000_000) {
      postfix = "G";
      value /= 1_000_000_000;
    } else if (value >= 1_000_000) {
      postfix = "M";
      value /= 1_000_000;
    } else if (value >= 1_000) {
      postfix = "k";
      value /= 1_000;
    } else postfix = "";

    return String.format("%.3f%s", value, postfix);
  }
}
