import model.Faction;
import model.Minion;
import model.World;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by dragoon on 24.11.16.
 */
public class XXX_AgressiveNeutralsCalcs {

	private HashMap<Long, Minion> phantoms;

	private HashSet<Long> agressive;

	public XXX_AgressiveNeutralsCalcs() {
		phantoms = new HashMap<>();
		agressive = new HashSet<>();
	}

	public XXX_AgressiveNeutralsCalcs(HashMap<Long, Minion> phantoms, HashSet<Long> agressive) {
		this.phantoms = new HashMap<>(phantoms);
		this.agressive = new HashSet<>(agressive);
	}

	public void updateMap(World world) {
		for (Minion minion : world.getMinions()) {
			if (minion.getFaction() == Faction.NEUTRAL) {
				Minion phantom = phantoms.get(minion.getId());
				if (phantom == null) {
					phantoms.put(minion.getId(), minion);
				} else {
					if (!agressive.contains(minion.getId()) && XXX_Utils.isUnitActive(phantom, minion)) {
						agressive.add(minion.getId());
					}
				}
			}
		}
	}

	public boolean isMinionAgressive(long id) {
		return agressive.contains(id);
	}

	public XXX_AgressiveNeutralsCalcs makeClone() {
		return new XXX_AgressiveNeutralsCalcs(phantoms, agressive);
	}
}
