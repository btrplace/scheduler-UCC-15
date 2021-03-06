/*
 * Copyright (c) 2014 University Nice Sophia Antipolis
 *
 * This file is part of btrplace.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.btrplace.scheduler.choco.view.net;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.VMState;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.Slice;
import org.btrplace.scheduler.choco.SliceBuilder;
import org.btrplace.scheduler.choco.transition.KeepRunningVM;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.transition.VMTransitionBuilder;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Arithmetic;
import org.chocosolver.solver.constraints.Operator;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;


/**
 * Model an action that allow a running VM to be relocate elsewhere if necessary.
 * The relocation can be performed through a live-migration or a re-instantiation.
 * The re-instantiation consists in forging a new VM having the same characteristics
 * and launching it on the destination node. Once this new VM has been launched, the
 * original VM is shut down. Such a relocation n may be faster than a migration-based
 * one while being less aggressive for the network. However, the VM must be able to
 * be cloned from a template.
 * <p>
 * If the relocation is performed with a live-migration, a {@link org.btrplace.plan.event.MigrateVM} action
 * will be generated. If the relocation is performed through a re-instantiation, a {@link org.btrplace.scheduler.choco.transition.ForgeVM},
 * a {@link org.btrplace.scheduler.choco.transition.BootVM}, and a {@link org.btrplace.scheduler.choco.transition.ShutdownVM} actions are generated.
 * <p>
 * To relocate the VM using a re-instantiation, the VM must first have an attribute {@code clone}
 * set to {@code true}. The re-instantiation duration is then estimated. If it is shorter than
 * the migration duration, then re-instantiation will be preferred.
 * <p>
 *
 * @author Fabien Hermenier
 */
public class MigrateVMTransition implements KeepRunningVM {

    public static final String PREFIX = "migrate(";
    public static final String PREFIX_STAY = "stayRunningOn(";
    private VM vm;
    private Slice cSlice, dSlice;
    private ReconfigurationProblem rp;
    private BoolVar state;
    private IntVar  start, end, duration, bandwidth;
    private BoolVar stay;
    private Node src;
    private boolean manageable = true;
    private boolean postCopy = false;

    /**
     * Make a new model.
     *
     * @param p the RP to use as a basis.
     * @param e the VM managed by the action
     * @throws org.btrplace.scheduler.SchedulerException if an error occurred
     */
    public MigrateVMTransition(ReconfigurationProblem p, VM e) throws SchedulerException {

        this.vm = e;
        this.rp = p;

        Solver s = rp.getSolver();
        Model mo = rp.getSourceModel();

        // Get src node
        src = p.getSourceModel().getMapping().getVMLocation(e);

        // Do not migrate
        stay = VariableFactory.zero(rp.getSolver());
        if (!p.getManageableVMs().contains(e)) {
            IntVar host = rp.makeCurrentHost(vm, PREFIX_STAY, vm, ").host");
            cSlice = new SliceBuilder(rp, vm, PREFIX_STAY, vm.toString(), ").cSlice")
                    .setHoster(host)
                    .setEnd(rp.makeUnboundedDuration(PREFIX_STAY, vm, ").cSlice_end"))
                    .build();
            dSlice = new SliceBuilder(rp, vm, PREFIX_STAY, vm, ").dSlice")
                    .setHoster(host)
                    .setStart(cSlice.getEnd())
                    .build();
            s.post(new Arithmetic(dSlice.getHoster(), Operator.EQ, cSlice.getHoster().getValue()));
            stay = VariableFactory.one(rp.getSolver());
            manageable = false;
            return;
        }

        // Migrate
        cSlice = new SliceBuilder(p, e, PREFIX, e, ").cSlice")
                .setHoster(p.getNode(p.getSourceModel().getMapping().getVMLocation(e)))
                .setEnd(p.makeUnboundedDuration(PREFIX, e, ").cSlice_end"))
                .build();

        dSlice = new SliceBuilder(p, vm, PREFIX, vm, ").dSlice")
                .setStart(p.makeUnboundedDuration(PREFIX, vm, ").dSlice_start"))
                .build();

        s.post(new Arithmetic(dSlice.getEnd(), Operator.LE, p.getEnd()));
        s.post(new Arithmetic(cSlice.getEnd(), Operator.LE, p.getEnd()));

        start = dSlice.getStart();
        end = cSlice.getEnd();

        // Duration from evaluator
        //DurationEvaluators dev = rp.getDurationEvaluators();
        //int migrateDuration = dev.evaluate(rp.getSourceModel(), org.btrplace.plan.event.MigrateVM.class, vm);

        // Get the networking view
        NetworkView network = (NetworkView) mo.getView(NetworkView.VIEW_ID);
        if (network == null) {
            throw new SchedulerException(rp.getSourceModel(), "View '" + NetworkView.VIEW_ID + "' is required but missing");
        }

        // Set the migration algorithm
        if (mo.getAttributes().isSet(vm, "postCopy")) {
            if (mo.getAttributes().getBoolean(vm, "postCopy")) postCopy = true;
        }

        //VariableFactory.task(start, duration, end);
    }

    public boolean usesPostCopy() { return postCopy; }

    private static String prettyMethod(IntVar method) {
            return "migration";
    }

    public IntVar getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(IntVar bw) {
        bandwidth = bw;
    }

    @Override
    public boolean isManaged() {
        return manageable;
    }

    @Override
    public VMState getSourceState() {
        return VMState.RUNNING;
    }

    @Override
    public VMState getFutureState() {
        return VMState.RUNNING;
    }

    @Override
    public boolean insertActions(Solution s, ReconfigurationPlan plan) {
        if (s.getIntVal(cSlice.getHoster()) != s.getIntVal(dSlice.getHoster())) {
            assert s.getIntVal(stay) == 0;
            Action a;
            Node dst = rp.getNode(s.getIntVal(dSlice.getHoster()));
            int st = s.getIntVal(getStart());
            int ed = s.getIntVal(getEnd());
            int bw = getBandwidth().getValue();
            a = new org.btrplace.plan.event.MigrateVM(vm, src, dst, st, ed, bw);
            plan.add(a);
        }
        return true;
    }

    @Override
    public VM getVM() { return vm; }

    @Override
    public IntVar getStart() { return start; }

    @Override
    public IntVar getEnd() {
        return end;
    }

    @Override
    public IntVar getDuration() {
        return duration;
    }

    public void setDuration(IntVar dur) {
        duration = dur;
    }

    @Override
    public Slice getCSlice() {
        return cSlice;
    }

    @Override
    public Slice getDSlice() {
        return dSlice;
    }

    @Override
    public BoolVar getState() {
        return state;
    }

    @Override
    public BoolVar isStaying() {
        return stay;
    }

    @Override
    public String toString() {
        return "migrate(" +
                "vm=" + vm +
                ", from=" + src + "(" + rp.getNode(src) + ")" +
                ", to=" + dSlice.getHoster().toString() + ")";
    }

    /**
     * The builder devoted to a running->running transition.
     */
    public static class Builder extends VMTransitionBuilder {

        /**
         * New builder
         */
        public Builder() {
            super("migrate", VMState.RUNNING, VMState.RUNNING);
        }

        @Override
        public VMTransition build(ReconfigurationProblem r, VM v) throws SchedulerException {
            return new MigrateVMTransition(r, v);
        }
    }
}
