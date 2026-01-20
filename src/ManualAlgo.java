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

    public ManualAlgo() { }

    @Override
    public String getInfo() {
        return "Manual control of Pac-Man using the keyboard (w,a,x,d).";
    }

    @Override
    public int move(PacmanGame game) {
        int ans = PacmanGame.ERR;

        Character cmd = Ex3Main.getCMD();
        if (cmd != null) {
            cmd = Character.toLowerCase(cmd);

            if (cmd == 'w') ans = PacmanGame.UP;
            else if (cmd == 'x') ans = PacmanGame.DOWN;
            else if (cmd == 'a') ans = PacmanGame.LEFT;
            else if (cmd == 'd') ans = PacmanGame.RIGHT;
        }

        return ans;
    }
}
