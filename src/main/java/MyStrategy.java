import model.Game;
import model.Move;
import model.Wizard;
import model.World;

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
        if (Constants.getGame() == null) {
            Constants.init(game, self);
            strategy = draw ? new Drawing_DrawingStrategy() : new StrategyImplement();
        }
        strategy.move(self, world, game, move);
    }
}
