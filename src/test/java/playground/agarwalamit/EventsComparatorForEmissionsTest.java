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

package playground.agarwalamit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.emissions.onRoadExposure.EventsComparatorForEmissions;

/**
 * Created by amit on 20.11.17.
 */

public class EventsComparatorForEmissionsTest {

    @Test
    public void test(){

        Map<ColdPollutant, Double> coldPollutantDoubleMap = new HashMap<>();
        coldPollutantDoubleMap.put(ColdPollutant.CO, 0.01);
        coldPollutantDoubleMap.put(ColdPollutant.NMHC, 0.01);
        coldPollutantDoubleMap.put(ColdPollutant.NO2, 0.01);
        coldPollutantDoubleMap.put(ColdPollutant.NOX, 0.01);
        coldPollutantDoubleMap.put(ColdPollutant.FC, 0.01);
        coldPollutantDoubleMap.put(ColdPollutant.HC, 0.01);
        coldPollutantDoubleMap.put(ColdPollutant.PM, 0.01);

        List<Event> eventList = new ArrayList<Event>();
        eventList.add(new PersonDepartureEvent(10.0, Id.createPersonId("OC7_X2P_E2I_7942"), Id.createLinkId("1973-2158-215210000-2163-2169"), "truck"));
        eventList.add(new PersonEntersVehicleEvent(10.0,Id.createPersonId("OC7_X2P_E2I_7942"),Id.create("OC7_X2P_E2I_7942_truck", Vehicle.class)));
        eventList.add(new PersonEntersVehicleEvent(10.0,Id.createPersonId("OC7_X2P_E2I_7943"),Id.create("OC7_X2P_E2I_7943_truck", Vehicle.class)));
        eventList.add(new ColdEmissionEvent(10.0, Id.createLinkId("1973-2158-215210000-2163-2169"),Id.create("OC7_X2P_E2I_7942_truck", Vehicle.class),coldPollutantDoubleMap));
        eventList.add(new VehicleEntersTrafficEvent(10.0,Id.createPersonId("OC7_X2P_E2I_7942"),Id.createLinkId("1973-2158-215210000-2163-2169"),Id.create("OC7_X2P_E2I_7942_truck", Vehicle.class),"truck", 1.0));
        eventList.add(new PersonArrivalEvent(20.0, Id.createPersonId("OC7_X2P_E2I_7942"),Id.createLinkId("1973-2158-215210000-2163-2169"), "truck"));
        eventList.add(new PersonDepartureEvent(11.0, Id.createPersonId("OC7_X2P_E2I_79342"), Id.createLinkId("1973-2158-215210000-2163-2169"), "truck"));

        System.out.println("Events before sorting ...");
        eventList.stream().forEach(e -> System.out.println(e.toString()));

        Collections.sort(eventList, new EventsComparatorForEmissions());

        System.out.println("Events after sorting ...");

        eventList.stream().forEach(e -> System.out.println(e.toString()));
        Event event = eventList.stream().filter(e->e.toString().contains(ColdEmissionEvent.EVENT_TYPE)).findFirst().get();

        Assert.assertEquals("Wrong event position", 4, eventList.indexOf(event));

        //
        eventList.clear();
        eventList.add(new WarmEmissionEvent(151.0,Id.createLinkId("1646710000-1645110000"),Id.createVehicleId("OC1_X2P_E2I_913_motorbike"), new HashMap<>()));
        eventList.add(new LinkLeaveEvent(151.0, Id.createVehicleId("OC1_X2P_E2I_913_motorbike"),Id.createLinkId("1646710000-1645110000") ));
        eventList.add(new LinkEnterEvent(151.0, Id.createVehicleId("OC1_X2P_E2I_913_motorbike"),Id.createLinkId("1645310000-2810000") ));

        eventList.add(new WarmEmissionEvent(151.0,Id.createLinkId("2206"),Id.createVehicleId("OC7_X2P_E2I_7937_truck"), new HashMap<>()));
        eventList.add(new LinkLeaveEvent(151.0, Id.createVehicleId("OC7_X2P_E2I_7937_truck"),Id.createLinkId("2206") ));
        eventList.add(new LinkEnterEvent(151.0, Id.createVehicleId("OC7_X2P_E2I_7937_truck"),Id.createLinkId("2205-220110000-1964-2768-2788-2785-278310000-2792-2742-2841") ));

        System.out.println("Events after sorting ...");
        Collections.sort(eventList, new EventsComparatorForEmissions());
        eventList.stream().forEach(e -> System.out.println(e.toString()));

        event = eventList.stream().filter(e->e.toString().contains(WarmEmissionEvent.EVENT_TYPE)).findFirst().get();

        Assert.assertEquals("Wrong event position", 2, eventList.indexOf(event));


    }
}