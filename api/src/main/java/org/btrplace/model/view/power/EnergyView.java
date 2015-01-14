package org.btrplace.model.view.power;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vins on 11/01/15.
 */
public class EnergyView implements ModelView, Cloneable {

    /**
     * The view identifier.
     */
    public static final String VIEW_ID = "EnergyView";

    public static final int DEFAULT_NODE_CONSUMPTION = 120; // 120 Watts
    public static final int DEFAULT_VM_CONSUMPTION = 20; // 20 Watts

    private String viewId;
    private int maxPower;
    Map<Node, Integer> nodeIdlePower;
    Map<VM, Integer> vmPower;

    public EnergyView(int maxPower) {
        this.viewId = VIEW_ID;
        this.maxPower = maxPower;
        nodeIdlePower = new HashMap<>();
        vmPower = new HashMap<>();
    }

    public void setConsumption(Node n, int power) {
        nodeIdlePower.put(n, power);
    }

    public void setConsumption(VM vm, int power) {
        vmPower.put(vm, power);
    }

    public int getConsumption(Node n) {
        return nodeIdlePower.getOrDefault(n, DEFAULT_NODE_CONSUMPTION);
    }

    public int getConsumption(VM vm) {
        return vmPower.getOrDefault(vm, DEFAULT_VM_CONSUMPTION);
    }

    public int getMaxPower() {
        return maxPower;
    }

    @Override
    public String getIdentifier() {
        return viewId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return this.viewId.equals(((EnergyView) o).getIdentifier());
    }

    @Override
    public ModelView clone() {
        EnergyView ev = new EnergyView(maxPower);
        return ev;
    }

    @Override
    public boolean substituteVM(VM curId, VM nextId) {
        return false;
    }
}
