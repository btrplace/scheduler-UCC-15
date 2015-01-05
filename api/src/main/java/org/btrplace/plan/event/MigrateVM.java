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

package org.btrplace.plan.event;

import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;

import java.util.Objects;

/**
 * Migrate a running VM from one online node to another one.
 *
 * @author Fabien Hermenier
 */
public class MigrateVM extends Action implements VMEvent, RunningVMPlacement {

    private VM vm;

    private Node src, dst;

    private int bw;

    /**
     * Make a new action.
     *
     * @param v         the VM to migrate
     * @param from      the node the VM is currently running on
     * @param to        the node where to place the VM
     * @param start     the moment the action will consume
     * @param end       the moment the action will stop
     * @param bandwidth the reserved bandwidth (negative value for unlimited bw)
     */
    public MigrateVM(VM v, Node from, Node to, int start, int end, int bandwidth) {
        super(start, end);
        this.vm = v;
        this.src = from;
        this.dst = to;
        this.bw = bandwidth;
    }

    public MigrateVM(VM v, Node from, Node to, int start, int end) {
        this(v, from, to, start, end, -1);
    }

    /**
     * Get the source node that is currently hosting the VM.
     *
     * @return the node identifier
     */
    public Node getSourceNode() {
        return src;
    }

    /**
     * Get the bandwidth reserved for the migration
     *
     * @return the bandwidth reserved
     */
    public int getBandwidth() {
        return bw;
    }

    @Override
    public Node getDestinationNode() {
        return dst;
    }

    @Override
    public VM getVM() {
        return vm;
    }

    /**
     * Make the VM running on the destination node
     * in the given model.
     *
     * @param i the model to alter with the action
     * @return {@code true} iff the VM is running on the destination node
     */
    @Override
    public boolean applyAction(Model i) {
        Mapping c = i.getMapping();
        if (c.isOnline(src)
                && c.isOnline(dst)
                && c.isRunning(vm)
                && c.getVMLocation(vm).equals(src)
                && !src.equals(dst)) {
            c.addRunningVM(vm, dst);
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        MigrateVM that = (MigrateVM) o;
        return this.vm.equals(that.vm) &&
                this.src.equals(that.src) &&
                this.dst.equals(that.dst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getEnd(), src, dst, vm);
    }

    @Override
    public String pretty() {
        String pretty = "migrate(vm=" + vm + ", from=" + src + ", to=" + dst;
        if(bw > 0) { pretty += ", bw=" + bw; }
        return pretty + ')';
    }

    @Override
    public Object visit(ActionVisitor v) {
        return v.visit(this);
    }
}
