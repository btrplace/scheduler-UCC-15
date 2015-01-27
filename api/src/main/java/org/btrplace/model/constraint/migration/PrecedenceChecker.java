package org.btrplace.model.constraint.migration;

import org.btrplace.model.constraint.AllowAllConstraintChecker;
import org.btrplace.model.constraint.migration.Precedence;

/**
 * Created by vkherbac on 23/01/15.
 */
public class PrecedenceChecker extends AllowAllConstraintChecker<Precedence> {

    /**
     * Make a new checker.
     *
     * @param p the constraint associated to the checker.
     */
    public PrecedenceChecker(Precedence p) {
        super(p);
    }

}
