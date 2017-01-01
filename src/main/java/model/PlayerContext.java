package model;

import java.util.Arrays;

public final class PlayerContext {
    private final Wizard[] wizards;
    private final World world;

    public PlayerContext(Wizard[] wizards, World world) {
        this.wizards = wizards == null ? null : Arrays.copyOf(wizards, wizards.length);
        this.world = world;
    }

    public Wizard[] getWizards() {
        return wizards == null ? null : Arrays.copyOf(wizards, wizards.length);
    }

    public World getWorld() {
        return world;
    }
}
