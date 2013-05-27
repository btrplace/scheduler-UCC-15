/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.model;

import java.util.Collection;
import java.util.Set;

/**
 * A mapping denotes the current state and placement of VMs and nodes.
 * These elements are identified integer values obtained by {@link btrplace.model.Model#newNode()}
 * and {@link btrplace.model.Model#newVM()}.
 *
 * @author Fabien Hermenier
 */
public interface Mapping extends Cloneable {

    /**
     * Set a virtual machine running on a node. The node must already be online.
     * If the virtual machine is already in a other location or state in the mapping, it is updated
     *
     * @param vm   the VM identifier, {@code >0}
     * @param node the node that will host the VM, {@code < 0}. The node must already be considered as online.
     * @return {@code true} if the vm is assigned on the node.
     */
    boolean addRunningVM(int vm, int node);

    /**
     * Set a virtual machine sleeping on a node.
     * If the virtual machine is already in a other location or state in the mapping, it is updated
     *
     * @param vm   the VM identifier, {@code >0}
     * @param node the node that will host the VM, {@code < 0}. The node must already be considered as online.
     * @return {@code false} if the hosting node is offline or unknown
     */
    boolean addSleepingVM(int vm, int node);

    /**
     * Set a VM ready for being running.
     * If the virtual machine is already in a other location or state in the mapping, it is updated
     *
     * @param vm the VM identifier, {@code > 0}
     */
    void addReadyVM(int vm);

    /**
     * Remove a VM.
     *
     * @param vm the VM to remove. {@code > 0}
     * @return {@code true} if the VM was in the mapping and has been removed
     */
    boolean removeVM(int vm);

    /**
     * Remove a node. The node must not host any virtual machines
     *
     * @param n the node identifier. {@code < 0}
     * @return {@code true} if the node was in the mapping and is removed. {@code false} otherwise
     */
    boolean removeNode(int n);

    /**
     * Get the list of nodes that are online.
     *
     * @return a list of nodes identifier, may be empty
     */
    Set<Integer> getOnlineNodes();

    /**
     * Set a node online. If the node is already in the mapping but in an another state, it is updated.
     *
     * @param node the node identifier. {@code < 0}
     */
    void addOnlineNode(int node);

    /**
     * Set a node offline. If the node is already in the mapping but in an another state, it is updated.
     * The node must not host any virtual machines
     *
     * @param node the node identifier. {@code < 0}
     * @return true if the node is offline. False otherwise
     */
    boolean addOfflineNode(int node);

    /**
     * Get the nodes that are offline.
     *
     * @return a set of node identifiers, may be empty
     */
    Set<Integer> getOfflineNodes();


    /**
     * Get the virtual machines that are running.
     *
     * @return a set of VM identifiers, may be empty
     */
    Set<Integer> getRunningVMs();

    /**
     * Get the virtual machines that are sleeping.
     *
     * @return a set of VM identifiers, may be empty
     */
    Set<Integer> getSleepingVMs();

    /**
     * Get the virtual machines that are sleeping on a node.
     *
     * @param n the node identifier. {@code < 0}
     * @return a set of VM identifier, may be empty
     */
    Set<Integer> getSleepingVMs(int n);

    /**
     * Get the VMs that are running on a node.
     *
     * @param n the node identifier. {@code < 0}
     * @return a set of VM identifier, may be empty
     */
    Set<Integer> getRunningVMs(int n);

    /**
     * Get the VMs that are ready.
     *
     * @return a set of VM identifier, may be empty
     */
    Set<Integer> getReadyVMs();

    /**
     * Get all the VMs involved in the mapping.
     *
     * @return a set of VM identifier, may be empty
     */
    Set<Integer> getAllVMs();

    /**
     * Get all the nodes involved in the mapping.
     *
     * @return a set of nodes, may be empty
     */
    Set<Integer> getAllNodes();

    /**
     * Get the location of a  running or a sleeping VM.
     *
     * @param vm the VM identifier. {@code > 0}
     * @return the identifier of the node hosting the virtual machine ({@code < 0}) or {@code 0} is the VM
     *         is not in the sleeping state nor the running state
     */
    int getVMLocation(int vm);

    /**
     * Get all the VMs running on a collection of nodes.
     *
     * @param ns a set of node identifier
     * @return a set of VM identifier, may be empty
     */
    Set<Integer> getRunningVMs(Collection<Integer> ns);

    /**
     * Copy a mapping.
     *
     * @return the created copy
     */
    Mapping clone();

    /**
     * Check if a VM is in the mapping.
     *
     * @param vm the VM identifier. {@code > 0}
     * @return {@code true} if the VM is in.
     */
    boolean containsVM(int vm);

    /**
     * Check if a node is in the mapping.
     *
     * @param node the node identifier. {@code < 0}
     * @return {@code true} if the node is in.
     */
    boolean containsNode(int node);

    /**
     * Remove all the nodes and the VMs in the mapping.
     */
    void clear();

    /**
     * Remove all the VMs remove on a given node
     *
     * @param u the node identifier. {@code < 0}
     */
    void clearNode(int u);

    /**
     * Remove all the VMs in the mapping
     */
    void clearAllVMs();
}
