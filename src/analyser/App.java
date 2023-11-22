package analyser;

import analyser.swing.MainUI;
import analyser.truss.Truss;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

public class App {

  private static Truss truss = new Truss();
  private static final Set<Truss> history = new HashSet<>();

  /**
   * @return the truss
   */
  public static Truss getTruss() {
    return truss;
  }

  public static void main(String[] args) {
    /* Set the Nimbus look and feel */
    /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
     * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
     */
    try {
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (
      ClassNotFoundException
      | InstantiationException
      | IllegalAccessException
      | javax.swing.UnsupportedLookAndFeelException ex
    ) {
      Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
    }

    java.awt.EventQueue.invokeLater(() -> new MainUI().setVisible(true));
  }

  public static void loadTruss(Truss newTruss) {
    history.add(truss);
    truss = newTruss;
  }

  public static void resetTruss() {
    history.add(truss);
    truss = new Truss();
  }
}
