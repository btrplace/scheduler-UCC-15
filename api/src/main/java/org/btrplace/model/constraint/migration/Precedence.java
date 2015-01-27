package org.btrplace.model.constraint.migration;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.SatConstraintChecker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by vkherbac on 23/01/15.
 */
public class Precedence extends SatConstraint {


    /**
     * Make a new constraint.
     *
     * @param v the involved VMs
     * @param n the involved nodes
     * @param c {@code true} to indicate a continuous restriction
     */
    public Precedence(Collection<VM> v, Collection<Node> n, boolean c) {
        super(v, n, c);
    }
    public Precedence(Collection<VM> vms) {
        super(vms, Collections.<Node>emptyList(), true);
    }

    public Precedence(VM... vms) {
        super(Arrays.asList(vms), Collections.<Node>emptyList(), true);
    }

    @Override
    public boolean setContinuous(boolean b) {
        return b;
    }

    @Override
    public SatConstraintChecker getChecker() {
        return new PrecedenceChecker(this);
    }

    @Override
    public String toString() {
        return "precedence(" + "vms=" + getInvolvedVMs() + ", " + restrictionToString() + ")";
    }
}
