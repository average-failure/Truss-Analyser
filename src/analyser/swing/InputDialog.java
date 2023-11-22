package analyser.swing;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.text.Format;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;

public abstract class InputDialog extends JDialog {

  private String value;

  private InputDialog(
    Frame parent,
    double x,
    double y,
    String title,
    String label,
    JFormattedTextField field
  ) {
    super(parent, title, Dialog.ModalityType.DOCUMENT_MODAL);
    setLayout(new FlowLayout());
    add(new JLabel(label));
    field.setPreferredSize(new Dimension(80, 30));
    field.addPropertyChangeListener(
      "value",
      e -> {
        if (field.getValue() == null) return;
        value = field.getText();
        dispose();
      }
    );
    add(field);
    ((JButton) add(new JButton("Submit"))).addActionListener(e -> {
        if (field.getValue() == null) return;
        value = field.getText();
        dispose();
      });
    pack();
    setResizable(false);
    setLocation((int) x, (int) y);
    setVisible(true);
  }

  protected InputDialog(
    Frame parent,
    double x,
    double y,
    String title,
    String label,
    Format format
  ) {
    this(parent, x, y, title, label, new JFormattedTextField(format));
  }

  protected InputDialog(
    Frame parent,
    double x,
    double y,
    String title,
    String label,
    AbstractFormatter format
  ) {
    this(parent, x, y, title, label, new JFormattedTextField(format));
  }

  public String getValue() {
    return value;
  }
}
