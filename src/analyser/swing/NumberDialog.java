package analyser.swing;

import java.awt.Frame;
import java.text.NumberFormat;

public class NumberDialog extends InputDialog {

  public NumberDialog(Frame parent, int x, int y) {
    super(
      parent,
      x,
      y,
      "Input Number",
      "Input a number: ",
      NumberFormat.getInstance()
    );
  }

  public int getIntValue() {
    return getValue() == null
      ? 0
      : (int) Math.round(Double.parseDouble(getValue()));
  }

  public double getDoubleValue() {
    return getValue() == null ? 0 : Double.parseDouble(getValue());
  }
}
