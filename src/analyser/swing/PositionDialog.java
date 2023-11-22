package analyser.swing;

import java.awt.Frame;
import java.text.NumberFormat;

public class PositionDialog extends InputDialog {

  public PositionDialog(Frame parent, int x, int y) {
    super(
      parent,
      x,
      y,
      "Input Position",
      "New Position: ",
      NumberFormat.getInstance()
    );
  }

  public double[] getDoubleValues() {
    if (getValue() == null) return new double[0];
    final String[] str = getValue().split(",");
    if (str.length != 2) return new double[0];
    return new double[] {
      Double.parseDouble(str[0]),
      Double.parseDouble(str[1]),
    };
  }
}
