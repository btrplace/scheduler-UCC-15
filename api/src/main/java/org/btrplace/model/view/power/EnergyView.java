package org.btrplace.model.view.power;

import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;

/**
 * Created by vins on 11/01/15.
 */
public class EnergyView implements ModelView, Cloneable {

    /**
     * The view identifier.
     */
    public static final String VIEW_ID = "EnergyView";

    private String viewId;
    private int maxPower;

    public EnergyView(int maxPower) {
        this.viewId = VIEW_ID;
        this.maxPower = maxPower;
    }

    public int getMaxPower() {
        return maxPower;
    }

    @Override
    public String getIdentifier() {
        return viewId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return this.viewId.equals(((EnergyView) o).getIdentifier());
    }

    @Override
    public ModelView clone() {
        EnergyView ev = new EnergyView(maxPower);
        return ev;
    }

    @Override
    public boolean substituteVM(VM curId, VM nextId) {
        return false;
    }
}
