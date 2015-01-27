package org.btrplace.model.constraint.migration;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.SatConstraintChecker;

import java.util.Collections;

/**
 * Created by vkherbac on 23/01/15.
 */
public class Deadline extends SatConstraint {

    private int deadline;

    /**
     * Make a new constraint.
     *
     * @param vm the VM to contraint
     */
    public Deadline(VM vm, int deadline) {
        super(Collections.singletonList(vm), Collections.<Node>emptyList(), true);
        this.deadline = deadline;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    @Override
    public boolean setContinuous(boolean b) {
        return b;
    }

    @Override
    public SatConstraintChecker getChecker() {
        return new DeadlineChecker(this);
    }

    @Override
    public String toString() {
        return "deadline(" + "vm=" + getInvolvedVMs() + ", deadline=" + deadline + ", " + restrictionToString() + ")";
    }
}
