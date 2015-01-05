package org.btrplace.model.view.net;

import java.util.List;

/**
 * Created by vkherbac on 05/01/15.
 */
public interface NetworkElement {

    int getCapacity();
    List<Port> getPorts();
}
