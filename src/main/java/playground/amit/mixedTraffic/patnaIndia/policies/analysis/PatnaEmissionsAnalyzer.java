/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.amit.mixedTraffic.patnaIndia.policies.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.EmissionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.amit.analysis.emission.EmissionUtilsExtended;
import playground.amit.analysis.emission.filtering.FilteredEmissionPersonEventHandler;
import playground.amit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.amit.utils.FileUtils;

/**
 * Created by amit on 23/12/2016.
 */

public class PatnaEmissionsAnalyzer {
    public static void main(String[] args) {
        String policyCase = "BT-mb";
        String emissionEventsFile = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/"+policyCase+"/output_emissions_events.xml.gz";
        String eventsFile = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/"+policyCase+"/output_events.xml.gz";
        String outEmissionFile = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/analysis/modalEmissions_"+policyCase+".txt";

        new PatnaEmissionsAnalyzer().run(emissionEventsFile, eventsFile, outEmissionFile);
    }

    private void run(final String emissionEventsFile, final String eventsFile, final String outFile){

        FilteredEmissionPersonEventHandler emissionPersonEventHandler = new FilteredEmissionPersonEventHandler();
////        FilteredColdEmissionHandler emissionPersonEventHandler = new FilteredColdEmissionHandler(30*3600.0, 1);

//        EmissionsPerPersonColdEventHandler emissionPersonEventHandler = new EmissionsPerPersonColdEventHandler();

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(emissionPersonEventHandler);
        events.addHandler(new Vehicle2DriverEventHandler());

        EmissionEventsReader emissionEventsReader = new EmissionEventsReader(events);
        emissionEventsReader.readFile(emissionEventsFile);

        // probably just use the total directly from event handler; (need to test first) amit June'17
        Map<Pollutant, Double> coldEmissions = EmissionUtilsExtended.getTotalEmissions(emissionPersonEventHandler.getPersonId2ColdEmissions(
                PatnaPersonFilter.PatnaUserGroup.urban.toString(),new PatnaPersonFilter()));
        Map<Pollutant, Double> warmEmissions = EmissionUtilsExtended.getTotalEmissions(emissionPersonEventHandler.getPersonId2WarmEmissions(PatnaPersonFilter.PatnaUserGroup.urban.toString(),new PatnaPersonFilter()));

        Map<Pollutant, Double> totalEmissions = new HashMap<>(coldEmissions);
        warmEmissions.forEach((k,v) -> totalEmissions.merge(k,v,Double::sum)) ;

//        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
//        try {
//            writer.write("pollutant \t valueInGm \n");
//            for (String str : totalEmissions.keySet()) {
//                writer.write(str+"\t"+totalEmissions.get(str)+"\n");
//            }
//            writer.close();
//        } catch (IOException e) {
//            throw new RuntimeException("Data is not written/read. Reason : " + e);
//        }

        Map<Id<Vehicle>, Map<Pollutant, Double>> vehicle2totalEmissions = EmissionUtils.sumUpEmissionsPerId(emissionPersonEventHandler.getVehicleId2WarmEmissions(), emissionPersonEventHandler.getVehicleId2ColdEmissions());
        SortedMap<String, SortedMap<Pollutant, Double>> mode2emissions=  getModalEmissions(vehicle2totalEmissions);

        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write("pollutant \t");
            for (Pollutant e : mode2emissions.get(TransportMode.car).keySet()) {
               writer.write(e.toString()+"\t");
            }
            writer.newLine();
            for (String str : mode2emissions.keySet()) {
                writer.write(str +"\t");
                for (Pollutant e : mode2emissions.get(str).keySet()) {
                    writer.write( mode2emissions.get(str).get(e)+"\t");
                }
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    private SortedMap<String, SortedMap<Pollutant, Double>> getModalEmissions(final Map<Id<Vehicle>, Map<Pollutant, Double>> vehicle2emissions){
       SortedMap<String, SortedMap<Pollutant, Double>> mode2emissions = new TreeMap<>();
       for (Id<Vehicle> vehicleId : vehicle2emissions.keySet()) {
           String mode = getModeFromVehicleIdForPatna(vehicleId.toString());
           SortedMap<Pollutant, Double> emissions = mode2emissions.get(mode);
           if (emissions == null) {
               emissions = new TreeMap<>(vehicle2emissions.get(vehicleId));
               mode2emissions.put(mode, emissions);
           } else {
               for (Map.Entry<Pollutant, Double> e : vehicle2emissions.get(vehicleId).entrySet()) {
                   emissions.put(e.getKey(), e.getValue() + emissions.get(e.getKey()) );
               }
           }
       }
       return mode2emissions;
    }

    private String getModeFromVehicleIdForPatna(final String vehicleIdString) {
        if( vehicleIdString.endsWith("motorbike") ){
            return "motorbike";
        } else if ( vehicleIdString.endsWith("bike") ){
            return TransportMode.bike;
        } else if ( vehicleIdString.endsWith("truck") ){
            throw new RuntimeException("Truck should not appear here becuase estimating emissions for urban travelers only");
//            return "truck";
        } else {
            return TransportMode.car;
        }
    }

}
