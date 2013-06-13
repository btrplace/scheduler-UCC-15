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

package btrplace.json.model.constraint;

import btrplace.json.JSONConverterException;
import btrplace.model.constraint.Running;
import net.minidev.json.JSONObject;

/**
 * JSON Converter for the constraint {@link btrplace.model.constraint.Running}.
 *
 * @author Fabien Hermenier
 */
public class RunningConverter extends ConstraintConverter<Running> {


    @Override
    public Class<Running> getSupportedConstraint() {
        return Running.class;
    }

    @Override
    public String getJSONId() {
        return "running";
    }

    @Override
    public Running fromJSON(JSONObject o) throws JSONConverterException {
        checkId(o);
        return new Running(requiredVMs(o, "vms"));
    }

    @Override
    public JSONObject toJSON(Running o) {
        JSONObject c = new JSONObject();
        c.put("id", getJSONId());
        c.put("vms", vmsToJSON(o.getInvolvedVMs()));
        return c;
    }
}
