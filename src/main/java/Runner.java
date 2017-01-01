import model.Game;
import model.Move;
import model.PlayerContext;
import model.Wizard;

import java.io.IOException;

public final class Runner {
    private final RemoteProcessClient remoteProcessClient;
    private final String token;

    private static boolean draw = false;

    public static void main(String[] args) throws IOException {
        if (args.length == 4) {
            draw = Boolean.parseBoolean(args[3]);
        }
        new Runner(args.length >= 3 ? args : new String[]{"127.0.0.1", "31001", "0000000000000000"}).run();
    }

    private Runner(String[] args) throws IOException {
        remoteProcessClient = new RemoteProcessClient(args[0], Integer.parseInt(args[1]));
        token = args[2];
    }

    public void run() throws IOException {
        try {
            remoteProcessClient.writeToken(token);
            remoteProcessClient.writeProtocolVersion();
            int teamSize = remoteProcessClient.readTeamSize();
            Game game = remoteProcessClient.readGameContext();

            Strategy[] strategies = new Strategy[teamSize];

            for (int strategyIndex = 0; strategyIndex < teamSize; ++strategyIndex) {
                strategies[strategyIndex] = new MyStrategy(draw);
            }

            PlayerContext playerContext;

            while ((playerContext = remoteProcessClient.readPlayerContext()) != null) {
                Wizard[] playerWizards = playerContext.getWizards();
                if (playerWizards == null || playerWizards.length != teamSize) {
                    break;
                }

                Move[] moves = new Move[teamSize];

                for (int wizardIndex = 0; wizardIndex < teamSize; ++wizardIndex) {
                    Wizard playerWizard = playerWizards[wizardIndex];

                    Move move = new Move();
                    moves[wizardIndex] = move;
                    strategies[wizardIndex /*playerWizard.getTeammateIndex()*/].move(
                            playerWizard, playerContext.getWorld(), game, move
                    );
                }

                remoteProcessClient.writeMoves(moves);
            }
        } finally {
            remoteProcessClient.close();
        }
    }
}
