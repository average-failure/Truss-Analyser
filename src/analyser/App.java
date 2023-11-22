package analyser;

import analyser.swing.MainUI;
import analyser.truss.Truss;
import java.util.HashSet;
import java.util.Set;

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
