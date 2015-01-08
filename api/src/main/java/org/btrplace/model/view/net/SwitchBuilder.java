package org.btrplace.model.view.net;

/**
 * Created by vkherbac on 05/01/15.
 */
public interface SwitchBuilder {

    /**
     * Generate a new non-blocking Switch.
     *
     * @return {@code null} if no identifiers are available for the Switch.
     */
    Switch newSwitch();

    /**
     * Generate a new Switch.
     *
     * @param id    the identifier to use for the Switch.
     * @return      a Switch or {@code null} if the identifier is already used.
     */
    Switch newSwitch(int id, int capacity);

    /**
     * Generate a new Switch.
     *
     * @param capacity the maximal BW capacity of the switch
     * @return {@code null} if no identifiers are available for the Switch.
     */
    Switch newSwitch(int capacity);

    /**
     * Check if a given Switch has been defined fot this network view.
     *
     * @param   sw the Switch to check.
     * @return  {@code true} if the Switch already exists.
     */
    boolean contains(Switch sw);

    /**
     * Clone the builder.
     *
     * @return a new switch builder.
     */
    SwitchBuilder clone();
}
