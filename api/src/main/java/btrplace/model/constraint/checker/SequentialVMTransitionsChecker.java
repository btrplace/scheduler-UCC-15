package btrplace.model.constraint.checker;

import btrplace.model.Model;
import btrplace.model.constraint.SequentialVMTransitions;
import btrplace.plan.event.*;

import java.util.*;

/**
 * Checker for the {@link btrplace.model.constraint.SequentialVMTransitions} constraint
 *
 * @author Fabien Hermenier
 * @see btrplace.model.constraint.SequentialVMTransitions
 */
public class SequentialVMTransitionsChecker extends AllowAllConstraintChecker<SequentialVMTransitions> {

    private Set<UUID> runnings;

    private List<UUID> order;

    /**
     * Make a new checker.
     *
     * @param s the associated constraint
     */
    public SequentialVMTransitionsChecker(SequentialVMTransitions s) {
        super(s);
        order = new ArrayList<>(s.getInvolvedVMs());
    }

    @Override
    public boolean start(BootVM a) {
        if (runnings.contains(a.getVM())) {
            return wasNext(a.getVM());
        }
        return true;
    }

    @Override
    public boolean startsWith(Model mo) {
        runnings = new HashSet<>(mo.getMapping().getRunningVMs());
        track(runnings);
        return true;
    }

    @Override
    public boolean start(MigrateVM a) {
        //If the VM belong to the order we remove it
        order.remove(a.getVM());
        return true;
    }

    private boolean wasNext(UUID vm) {
        if (getVMs().contains(vm)) {
            while (!order.isEmpty()) {
                if (order.remove(0).equals(vm)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean start(ShutdownVM a) {
        return wasNext(a.getVM());
    }

    @Override
    public boolean start(ResumeVM a) {
        return wasNext(a.getVM());
    }

    @Override
    public boolean start(SuspendVM a) {
        return wasNext(a.getVM());
    }

    @Override
    public boolean start(KillVM a) {
        //TODO: only if was not ready
        return wasNext(a.getVM());
    }
}
