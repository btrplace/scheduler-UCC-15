package org.btrplace.scheduler.choco.constraint.migration;

import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.model.constraint.migration.Deadline;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraint;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.variables.VF;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vkherbac on 23/01/15.
 */
public class CDeadline implements ChocoConstraint {

    private Deadline dl;
    private List<MigrateVMTransition> migrationList;

    public CDeadline(Deadline dl) {
        this.dl = dl;
        migrationList = new ArrayList<>();
    }

    private int convertTimestamp(String timestamp) throws ParseException {

        // Get the deadline from timestamp
        int deadline;
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
        Date parsedDate = null;

        // Relative timestamp
        if (timestamp.startsWith("+")) {
            parsedDate = dateFormat.parse(timestamp.replace("+", ""));
            Calendar c = Calendar.getInstance();
            c.setTime(parsedDate);
            deadline = (c.get(Calendar.SECOND) + c.get(Calendar.MINUTE) * 60 + c.get(Calendar.HOUR_OF_DAY) * 3600);
        }
        // Absolute timestamp
        else {
            Calendar c = Calendar.getInstance();
            c.setTime(new Date());
            Date now = dateFormat.parse(c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND));
            parsedDate = dateFormat.parse(timestamp);
            deadline = (int) ((parsedDate.getTime() - now.getTime()) / 1000);
            if (deadline < 0) {
                // Timestamp is for tomorrow
                deadline = (int) (long) ((parsedDate.getTime() + (24 * 3600 * 1000) - now.getTime()) / 1000);
            }
        }

        return deadline;
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SchedulerException {

        // Get the solver
        Solver s = rp.getSolver();

        int deadline = 0;
        try {
            deadline = convertTimestamp(dl.getTimestamp());
        } catch (ParseException e) {
            throw new SchedulerException(rp.getSourceModel(), "Unable to parse the timestamp '" + dl.getTimestamp() + "'");
        }

        // Get all migrations involved
        for (Iterator<VM> ite = dl.getInvolvedVMs().iterator(); ite.hasNext();) {
            VM vm = ite.next();
            VMTransition vt = rp.getVMAction(vm);
            if (vt instanceof MigrateVMTransition) {
                LCF.ifThen(VF.not(((MigrateVMTransition)vt).isStaying()), ICF.arithm(vt.getEnd(), "<=", deadline));
            }
        }

        return true;
    }

    @Override
    public Set<VM> getMisPlacedVMs(Model m) {
        return Collections.emptySet();
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoConstraintBuilder {
        @Override
        public Class<? extends Constraint> getKey() {
            return Deadline.class;
        }

        @Override
        public CDeadline build(Constraint c) {
            return new CDeadline((Deadline) c);
        }
    }
}
