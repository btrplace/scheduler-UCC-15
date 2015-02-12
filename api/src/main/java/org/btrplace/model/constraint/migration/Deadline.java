package org.btrplace.model.constraint.migration;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.SatConstraintChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by vkherbac on 23/01/15.
 */
public class Deadline extends SatConstraint {

    private String timestamp;

    /**
     * Instantiate discrete constraints for a collection of VMs.
     *
     * @param vms   the VMs to integrate
     * @param deadline the desired deadline
     * @return the associated list of constraints
     */
    public static List<Deadline> newDeadline(Collection<VM> vms, String deadline) {
        List<Deadline> l = new ArrayList<>(vms.size());
        for (VM v : vms) {
            l.add(new Deadline(v, deadline));
        }
        return l;
    }

    /**
     * Make a new constraint.
     *
     * @param vm the VM to contraint
     * @param timestamp the desired deadline
     */
    public Deadline(VM vm, String timestamp) {
        super(Collections.singletonList(vm), Collections.<Node>emptyList(), true);
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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
        return "deadline(" + "vm=" + getInvolvedVMs() + ", deadline='" + timestamp + "', " + restrictionToString() + ")";
    }
}
