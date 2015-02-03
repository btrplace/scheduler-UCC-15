package org.btrplace.plan.gantt;

import org.btrplace.plan.event.Action;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Created by vkherbac on 15/01/15.
 */
public final class ActionsToCSV {

    public static char SEPARATOR = ',';

    public static boolean convert(Set<Action> actionsSet, String outputFile) {

        if(actionsSet.isEmpty()) return false;

        // From Set to List
        List<Action> actions = new ArrayList<>();
        actions.addAll(actionsSet);

        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8"));

            // Sort the actions per start and end times
            actions.sort(new Comparator<Action>() {
                @Override
                public int compare(Action action, Action action2) {
                    int result = action.getStart() - action2.getStart();
                    if (result == 0) { result = action.getEnd() - action2.getEnd(); }
                    return result;
                }
            });

            // Write header
            writer.write("VM"+SEPARATOR+"NODE"+SEPARATOR+"Start"+SEPARATOR+"End");

            for (Action a : actions) {
                writer.newLine();

                String descr = a.pretty();

                int start=a.getStart(), end=a.getEnd();
                String vm=null, node=null, nodeDst=null;

                // Boot
                if (descr.contains("boot")) {
                    node = descr.replace("boot(node=", "").replace(")", "");
                    vm = "boot_" + node;
                }
                // Shutdown
                else if(descr.contains("shutdown")) {
                    node = descr.replace("shutdown(node=", "").replace(")", "");
                    vm = "shutdown_" + node;
                }
                // Migration
                else if(descr.contains("migrate")) {
                    vm = descr.substring(0, descr.indexOf(",")).replace("migrate(vm=","");
                    node = descr.substring(descr.indexOf(","),descr.indexOf(",", descr.indexOf(",")+1)).replace(",", "")
                            .replace(" from=", "");
                    //nodeDst = descr.substring(descr.indexOf(",",descr.indexOf(",")+1),descr.lastIndexOf(","))
                    // .replace(",","");
                }
                else {
                    try {writer.close(); return false;} catch (Exception e) {return false;}
                }

                writer.write(vm+SEPARATOR+node+SEPARATOR+Integer.toString(start)+SEPARATOR+Integer.toString(end));
            }

            writer.flush();
        } catch (IOException ex) {}
        finally {
            try {writer.close(); return true;} catch (Exception e) {return false;}
        }
    }
}
