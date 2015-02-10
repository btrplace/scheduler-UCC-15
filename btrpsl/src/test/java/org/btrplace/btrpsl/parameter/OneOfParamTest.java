package org.btrplace.btrpsl.parameter;

import org.btrplace.btrpsl.constraint.ListOfParam;
import org.btrplace.btrpsl.constraint.OneOfParam;
import org.btrplace.btrpsl.constraint.StringParam;
import org.btrplace.btrpsl.element.BtrpOperand;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by vkherbac on 10/02/15.
 */
@Test
public class OneOfParamTest {

    public void testInstantiation() {

        OneOfParam op = new OneOfParam("$oneOf", new StringParam("$date"), new ListOfParam("$vms", 1, BtrpOperand.Type.VM, false));
        Assert.assertNotNull(op);
        Assert.assertEquals(op.prettySignature(), "string || set<VM>");
    }
}
