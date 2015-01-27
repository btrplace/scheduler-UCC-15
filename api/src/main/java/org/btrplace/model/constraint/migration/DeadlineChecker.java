package org.btrplace.model.constraint.migration;

import org.btrplace.model.constraint.AllowAllConstraintChecker;
import org.btrplace.model.constraint.migration.Deadline;

/**
 * Created by vkherbac on 23/01/15.
 */
public class DeadlineChecker extends AllowAllConstraintChecker<Deadline> {

    /**
     * Make a new checker.
     *
     * @param dl the constraint associated to the checker.
     */
    public DeadlineChecker(Deadline dl) {
        super(dl);
    }
}