package org.btrplace.model.view.power;

import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;

/**
 * Created by vins on 11/01/15.
 */
public class PowerView implements ModelView, Cloneable {

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public ModelView clone() {
        return null;
    }

    @Override
    public boolean substituteVM(VM curId, VM nextId) {
        return false;
    }
}
