package analyser.swing;

import java.awt.Frame;
import java.text.NumberFormat;

public class AddForceDialog extends InputDialog {

  protected AddForceDialog(Frame parent, double x, double y) {
    super(
      parent,
      x,
      y,
      "Add External Force",
      "Force Magnitude: ",
      NumberFormat.getInstance()
    );
  }

  public double getDoubleValue() {
    if (getValue() == null) return 0;
    return Double.parseDouble(getValue().replace(",", ""));
  }
}
