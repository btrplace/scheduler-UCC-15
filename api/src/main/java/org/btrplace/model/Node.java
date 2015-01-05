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

package org.btrplace.model;

import org.btrplace.model.view.net.NetworkElement;
import org.btrplace.model.view.net.Port;

import java.util.ArrayList;
import java.util.List;

/**
 * Model a node.
 * A node should not be instantiated directly. Use {@link Model#newNode()} instead.
 *
 * @author Fabien Hermenier
 * @see Model#newNode()
 */
public class Node implements Element,NetworkElement {

    private int id;
    private int capacity;
    private List<Port> ports;

    /**
     * Make a new node.
     *
     * @param i the node identifier.
     */
    public Node(int i) {
        this.id = i;
        this.capacity = 0;
        ports = new ArrayList<>();
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String toString() {
        return "node#" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Node)) {
            return false;
        }

        Node node = (Node) o;

        return id == node.id();
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int getCapacity() {
        capacity = 0;
        if (!ports.isEmpty()) {
            for (Port p : ports) {
                capacity += p.getBandwidth();
            }
        }
        return capacity*2; // *2: IN+OUT
    }

    @Override
    public List<Port> getPorts() {
        return ports;
    }
}
