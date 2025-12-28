import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

/**
 * ManualAlgo
 *
 * This class allows the user to control Pac-Man manually
 * using the keyboard.
 *
 * Controls:
 * w - move up
 * a - move left
 * x - move down
 * d - move right
 */
public class ManualAlgo implements PacManAlgo {

    /**
     * Default constructor.
     */
    public ManualAlgo() { }

    /**
     * Returns a short description of this algorithm.
     */
    @Override
    public String getInfo() {
        return "Manual control of Pac-Man using the keyboard (w,a,x,d).";
    }

    /**
     * This method is called every game step.
     * It reads the last key pressed by the user
     * and moves Pac-Man accordingly.
     */
    @Override
    public int move(PacmanGame game) {
        int ans = PacmanGame.ERR;

        // Get the last key pressed
        Character cmd = Ex3Main.getCMD();

        if (cmd != null) {
            if (cmd == 'w') { ans = PacmanGame.UP; }
            if (cmd == 'x') { ans = PacmanGame.DOWN; }
            if (cmd == 'a') { ans = PacmanGame.LEFT; }
            if (cmd == 'd') { ans = PacmanGame.RIGHT; }
        }

        return ans;
    }
}
