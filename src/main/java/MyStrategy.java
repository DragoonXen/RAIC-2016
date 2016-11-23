import model.Game;
import model.Move;
import model.Wizard;
import model.World;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class MyStrategy implements Strategy {

    private boolean draw;

    public MyStrategy() {
        draw = false;
    }

    public MyStrategy(boolean draw) {
        this.draw = draw;
    }

    private StrategyImplement strategy;

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        try {
            if (Constants.getGame() == null) {
                Constants.init(game, self);
                strategy = draw ? new Drawing_DrawingStrategy(self) : new StrategyImplement(self);
            }
            strategy.move(self, world, game, move);
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
            pw.close();
            try {
                sw.close();
            } catch (IOException e1) {
                System.out.println(e1.getMessage());
            }
        }
    }
}
