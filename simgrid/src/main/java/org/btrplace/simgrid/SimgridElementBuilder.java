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

package org.btrplace.simgrid;

import org.btrplace.model.ElementBuilder;
import org.btrplace.model.Node;
import org.btrplace.model.VM;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link org.btrplace.model.ElementBuilder}.
 * For a thread-safe implementation, see {@link org.btrplace.model.SynchronizedElementBuilder}
 *
 * @author Fabien Hermenier
 */
public class SimgridElementBuilder implements ElementBuilder {

    private BitSet usedVMIds;

    private BitSet usedNodeIds;

    private int nextNodeId;

    private int nextVMId;

    private Map<String, Node> nodes;

    /**
     * New builder.
     */
    public SimgridElementBuilder() {
        usedNodeIds = new BitSet();
        usedVMIds = new BitSet();
        nodes = new HashMap<>();
    }

    public Node newNode(String name) {
        if (!nodes.containsKey(name)) {
            Node n = newNode();
            nodes.put(name, n);
            return n;
        }
        return null;
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public Node getNode(String name) {
        if(nodes.containsKey(name)) {
            return nodes.get(name);
        }
        return null;
    }

    @Override
    public VM newVM() {
        int id = nextVMId++;
        if (id < 0) {
            //We look for holes in the bitset
            id = usedVMIds.nextClearBit(0);
        }
        usedVMIds.set(id);
        return new VM(id);
    }

    @Override
    public Node newNode() {
        int id = nextNodeId++;
        if (id < 0) {
            //We look for holes in the bitset
            id = usedNodeIds.nextClearBit(0);
        }
        usedNodeIds.set(id);
        return new Node(id);
    }

    @Override
    public VM newVM(int id) {
        if (!usedVMIds.get(id)) {
            usedVMIds.set(id);
            nextVMId = Math.max(nextVMId, id + 1);
            return new VM(id);
        }
        return null;
    }

    @Override
    public Node newNode(int id) {
        if (!usedNodeIds.get(id)) {
            usedNodeIds.set(id);
            nextVMId = Math.max(nextVMId, id + 1);
            return new Node(id);
        }
        return null;
    }

    @Override
    public ElementBuilder clone() {
        SimgridElementBuilder c = new SimgridElementBuilder();
        c.nextVMId = nextVMId;
        c.nextNodeId = nextNodeId;
        c.usedVMIds = (BitSet) usedVMIds.clone();
        c.usedNodeIds = (BitSet) usedNodeIds.clone();
        c.nodes.putAll(nodes);
        return c;
    }

    public boolean containsNode(String name) {
        return nodes.containsKey(name);
    }

    @Override
    public boolean contains(VM v) {
        return usedVMIds.get(v.id());
    }

    @Override
    public boolean contains(Node n) {
        return usedNodeIds.get(n.id());
    }
}
