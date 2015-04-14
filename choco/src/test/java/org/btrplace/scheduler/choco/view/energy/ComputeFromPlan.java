package org.btrplace.scheduler.choco.view.energy;

import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vkherbac on 13/04/15.
 */
public class ComputeFromPlan {

    String path = new File("").getAbsolutePath() + "/choco/src/test/java/org/btrplace/scheduler/choco/view/energy/";
    
    @Test
    public void execute() throws IOException {

        //String plan = path + "socc-vanilla_deco.1.sorted.csv";
        //String plan = path + "socc_deco.2.sorted.csv";
        String plan = path + "socc_deco_cap.3.sorted.csv";
        String output = plan + ".energy";

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "utf-8"));
        writer.write("TIME" + ";" + "POWER");
        
        BufferedReader br = new BufferedReader(new FileReader(plan));
        String line = "";
        int line_nb = 0;
        int start=Integer.MAX_VALUE, end=0, current_start, current_end;
        while ((line = br.readLine()) != null) {
            line_nb++; if (line_nb == 1) { continue; }
            current_start = Integer.parseInt(line.split(";")[1]);
            if (current_start < start) start = current_start;
            current_end = Integer.parseInt(line.split(";")[2]);
            if (current_end > end) end = current_end;
        }
        
        //System.out.println("Start=" + start + " End="+ end );
        
        int nodes_nb = 48;
        int vms_nb = 96;
        int max_bw = 1000;
        int total_max_bw = 10000;
        
        String action = "";
        int start_action, end_action, power;
        for (int t=start-1; t<=end; t++) {

            writer.newLine();
            power = 0;
            String migrations = ""; int nb_migrations = 0;
            
            line_nb = 0;
            br = new BufferedReader(new FileReader(plan));
            while ((line = br.readLine()) != null) {
                line_nb++; if (line_nb == 1) continue;
                    
                action = line.split(";")[0];
                start_action = Integer.parseInt(line.split(";")[1]);
                end_action = Integer.parseInt(line.split(";")[2]);

                if (start_action <= t && end_action > t) {
                    if (action.contains("migrate")) {
                        migrations = migrations.concat(action + "\n");
                        nb_migrations ++;
                    }
                    if (action.contains("boot")) {
                        power+=20;
                    }
                }
                if (start_action == t) {
                    if (action.contains("boot")) {
                        nodes_nb++;
                    }
                }
                if (end_action == t) {
                    if (action.contains("shutdown")) {
                        nodes_nb--;
                    }
                }
            }
            
            if (!migrations.equals("")) {

                int total_bw = 0, mig_max_bw, new_bw, bw;

                List<Integer> max_bw_migs = new ArrayList<>();
                String mig = ""; BufferedReader migReader = new BufferedReader(new StringReader(migrations));
                while ((mig = migReader.readLine()) != null) {

                    mig_max_bw = max_bw;

                    String src_node = mig.split(",")[1].split("=")[1];
                    String dst_node = mig.split(",")[2].split("=")[1];
                    int nb_src_migs = 1, nb_dst_migs = 1;

                    String checkMig = ""; BufferedReader checkMigReader = new BufferedReader(new StringReader(migrations));
                    while ((checkMig = checkMigReader.readLine()) != null) {
                        if (mig.equals(checkMig)) continue;
                        String check_src_node = checkMig.split(",")[1].split("=")[1];
                        if (src_node.equals(check_src_node)) {
                            nb_src_migs++;
                        }
                        String check_dst_node = checkMig.split(",")[2].split("=")[1];
                        if (dst_node.equals(check_dst_node)) {
                            nb_dst_migs++;
                        }
                    }

                    mig_max_bw = mig_max_bw / Math.max(nb_src_migs, nb_dst_migs);
                    total_bw += mig_max_bw;
                    max_bw_migs.add(mig_max_bw);
                }

                for (int mb : max_bw_migs) {
                    bw = mb;
                    if (total_bw > total_max_bw) {
                        new_bw = total_max_bw / nb_migrations;
                        if (new_bw < bw) { bw = new_bw; }
                    }

                    power += ((bw / 16) + 20);
                    //System.out.println("BW=" + bw);
                }
            }
            
            power+=(nodes_nb * 110);
            power+=(vms_nb * 16);

            writer.write(Integer.toString(t-start) + ";" + Integer.toString(power));
        }
        
        writer.flush();

        try {
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
