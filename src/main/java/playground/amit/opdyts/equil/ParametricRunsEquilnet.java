/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.amit.opdyts.equil;

import playground.vsp.parametricRuns.PrepareParametricRuns;

/**
 * A class to create a job script, write it on remote and then run the job based on the given parameters.
 *
 * Created by amit on 04.10.17.
 */

public class ParametricRunsEquilnet {


    public static void main(String[] args) {
        int runCounter= 100;
        String baseDir = "/net/ils4/agarwal/equilOpdyts/carPt/output/";
        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","agarwal");

        String ascStyles [] = {"axial_fixedVariation","axial_randomVariation"};
        double [] stepSizes = {0.25, 0.5, 1.0};
        Integer [] convIterations = {300};
        double [] selfTuningWts = {1.0};
        Integer [] warmUpIts = {1, 5, 10};

        buffer.append("runNr\tascStyle\tstepSize\titerations2Convergence\tselfTunerWt\twarmUpIts")
              .append(PrepareParametricRuns.newLine);

        int cnt = runCounter;
        for (String ascStyle : ascStyles ) {
            for(double stepSize :stepSizes){
                for (int conIts : convIterations) {
                    for (double selfTunWt : selfTuningWts) {
                        for (int warmUpIt : warmUpIts) {
                            String arg = ascStyle + " "+ stepSize + " " + conIts + " " + selfTunWt + " " + warmUpIt;

                            String matsimDir = "r_d24e170ecef8172430381b23c72f39e3f9e79ea1_opdyts_22Oct";
                            String jobName = "run"+String.valueOf(runCounter++);

                            String [] additionalLines = {
                                    "echo \"========================\"",
                                    "echo \" "+matsimDir+" \" ",
                                    "echo \"========================\"",
                                    PrepareParametricRuns.newLine,

                                    "cd /net/ils4/agarwal/matsim/"+matsimDir+"/",
                                    PrepareParametricRuns.newLine,

                                    "java -Djava.awt.headless=true -Xmx29G -cp agarwalamit-0.10.0-SNAPSHOT.jar " +
                                            "playground/agarwalamit/opdyts/equil/MatsimOpdytsEquilIntegration " +
                                            "/net/ils4/agarwal/equilOpdyts/carPt/inputs/ " +
                                            "/net/ils4/agarwal/equilOpdyts/carPt/output/"+jobName+"/ " +
                                            "/net/ils4/agarwal/equilOpdyts/carPt/relaxedPlans/output_plans.xml.gz "+
                                            arg+" "
                            };


                            parametricRuns.run(additionalLines, baseDir, jobName);
                            buffer.append(jobName)
                                  .append("\t")
                                  .append(arg.replace(' ', '\t'))
                                  .append(PrepareParametricRuns.newLine);
                        }
                    }
                }
            }
        }

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseDir+"/runInfo.txt");
        parametricRuns.close();
    }
}
