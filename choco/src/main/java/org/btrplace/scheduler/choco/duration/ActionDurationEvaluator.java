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

package org.btrplace.scheduler.choco.duration;

import org.btrplace.model.Element;
import org.btrplace.model.Model;


/**
 * Interface to specify the duration evaluator for a possible action on an element.
 *
 * @author Fabien Hermenier
 */
public interface ActionDurationEvaluator<E extends Element> {

    /**
     * Evaluate the duration of the action on a given element.
     *
     * @param mo the model to consider
     * @param e  the VM
     * @return a positive integer
     */
    int evaluate(Model mo, E e);
}
