package org.btrplace.model.view.power;

import org.btrplace.model.constraint.AllowAllConstraintChecker;

/**
 * Created by vins on 11/01/15.
 */
public class PowerBudgetChecker extends AllowAllConstraintChecker<PowerBudget> {

    /**
     * Make a new checker.
     *
     * @param pb the constraint associated to the checker.
     */
    public PowerBudgetChecker(PowerBudget pb) {
        super(pb);
    }
}
