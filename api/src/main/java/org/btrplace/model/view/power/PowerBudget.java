package org.btrplace.model.view.power;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.SatConstraintChecker;

import java.util.Collections;

/**
 * Created by vins on 11/01/15.
 */
public class PowerBudget extends SatConstraint {

    private int budget, start, end;

    /**
     * Make a new constraint.
     *
     * @param budget    power budget allowed
     * @param start     the power restriction start at t=start
     * @param end       the end of power restriction
     */
    public PowerBudget(int budget, int start, int end) {
        super(Collections.<VM>emptyList(), Collections.<Node>emptyList(), true);
        this.budget = budget;
        this.start = start;
        this.end = end;
    }

    public int getBudget() {
        return budget;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public boolean setContinuous(boolean b) {
        return b;
    }

    @Override
    public SatConstraintChecker getChecker() {
        return new PowerBudgetChecker(this);
    }

    @Override
    public String toString() {
        return "powerBudget(" + "threshold=" + budget + ", interval=[" + start + ',' + end + "], " + restrictionToString() + ')';
    }
}
