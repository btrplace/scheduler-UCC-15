package btrplace.model.constraint.checker;

import btrplace.model.Model;
import btrplace.model.SatConstraint;
import btrplace.plan.RunningVMPlacement;
import btrplace.plan.event.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Fabien Hermenier
 */
public abstract class DenyMyVMsActions<C extends SatConstraint> implements SatConstraintChecker<C> {

    protected Set<UUID> vms;

    protected Set<UUID> nodes;

    private C cstr;

    public DenyMyVMsActions(C s) {
        cstr = s;
        vms = new HashSet<>(s.getInvolvedVMs());
    }

    @Override
    public boolean startsWith(Model mo) {
        return true;
    }

    @Override
    public boolean start(MigrateVM a) {
        return startRunningVMPlacement(a);
    }

    @Override
    public void end(MigrateVM a) {
        endRunningVMPlacement(a);
    }

    @Override
    public boolean start(BootVM a) {
        return startRunningVMPlacement(a);
    }

    @Override
    public void end(BootVM a) {
        endRunningVMPlacement(a);
    }

    @Override
    public boolean start(BootNode a) {
        return true;
    }

    @Override
    public void end(BootNode a) {

    }

    @Override
    public boolean start(ShutdownVM a) {
        return !vms.contains(a.getVM());
    }

    @Override
    public void end(ShutdownVM a) {

    }

    @Override
    public boolean start(ShutdownNode a) {
        return true;
    }

    @Override
    public void end(ShutdownNode a) {

    }

    @Override
    public boolean start(ResumeVM a) {
        return startRunningVMPlacement(a);
    }

    @Override
    public void end(ResumeVM a) {

    }

    @Override
    public boolean start(SuspendVM a) {
        return !vms.contains(a.getVM());
    }

    @Override
    public void end(SuspendVM a) {

    }

    @Override
    public boolean start(KillVM a) {
        return !vms.contains(a.getVM());
    }

    @Override
    public void end(KillVM a) {

    }

    @Override
    public boolean start(ForgeVM a) {
        return !vms.contains(a.getVM());
    }

    @Override
    public void end(ForgeVM a) {

    }

    @Override
    public boolean endsWith(Model mo) {
        return true;
    }

    @Override
    public boolean consume(SubstitutedVMEvent e) {
        return !vms.remove(e.getVM()) || vms.add(e.getNewUUID());
    }

    @Override
    public boolean consume(AllocateEvent e) {
        return true;
    }

    @Override
    public boolean start(Allocate e) {
        return true;
    }

    @Override
    public void end(Allocate e) {

    }

    public boolean startRunningVMPlacement(RunningVMPlacement a) {
        return !vms.contains(a.getVM());
    }

    public void endRunningVMPlacement(RunningVMPlacement a) {

    }

    @Override
    public C getConstraint() {
        return cstr;
    }
}
