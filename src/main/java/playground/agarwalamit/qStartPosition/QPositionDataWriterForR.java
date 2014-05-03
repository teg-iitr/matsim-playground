/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.qStartPosition;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author amit
 */
public class QPositionDataWriterForR {

	//	private static String configFile = "../../patnaIndiaSim/input/configTestCase.xml";//"./input/configTest.xml";
	//	private static String outputDir = "../../patnaIndiaSim/outputTestCase/3modesPassing/";//"./outputTest/";//
	//	private static String eventFile = outputDir+"ITERS/data_Patna_3modes_alternativeSpeed_events_Passing.xml";//outputDir+"/ITERS/it.10/10.events.xml.gz";//
//	private static String configFile ="../../patnaIndiaSim/outputSS/3links1Km/config.xml";
//	private static String outputDir ="../../patnaIndiaSim/outputSS/3links1Km/";
//	private static String eventFile = outputDir+"/events.xml";
//	private static String networkFile="../../patnaIndiaSim/outputSS/3links1Km/dreieck_network.xml";
	
		private static String configFile ="./output/config.xml";
		private static String outputDir = "./output/";
		private static String eventFile = outputDir+"events2000.xml";
		private static String networkFile = outputDir+"network.xml";

	private static Scenario scenario;
	private static QueuePositionCalculationHandler calculationHandler;

	private final static Logger logger = Logger.getLogger(QPositionDataWriterForR.class);

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(networkFile);
		scenario  = ScenarioUtils.loadScenario(config);

		calculationHandler = new QueuePositionCalculationHandler(scenario);
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(calculationHandler);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(eventFile);
		writeLinkEnterLeaveQueuePosDataForR();
		writeLinkEnterLeaveTimeForR();
		logger.info("Writing file(s) is finished.");
	}

	private static void writeLinkEnterLeaveQueuePosDataForR(){
		List<String> qPositionData = calculationHandler.getPersonLinkEnterTimeVehiclePositionDataToWrite();
		List<String> linkEnterLeaveTimeData = calculationHandler.getPersonLinkEnterLeaveTimeDataToWrite();
		List<String> copyLinkEnterLeaveTimeData = new ArrayList<String>(linkEnterLeaveTimeData);
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/rDataPersonInQueueData4.txt");
		double vehicleSpeed =0;
		try {
			writer.write("personId \t linkId \t startTimeX1 \t initialPositionY1 \t endTimeX2 \t endPositionY2 \t travelMode \n");

			for(String qPosDataLine : qPositionData){
				String qParts[] =qPosDataLine.split("\t");

				String personId = qParts[0];
				String linkId = qParts[1];
				String linkEnterTime = qParts[2];
				String queuingTime =qParts[3];
				String linkLength = qParts[4];
				String travelMode = qParts[5];
				String linkLeaveTime = qParts[6];

				vehicleSpeed=getVehicleSpeed(travelMode);

				double initialPos = Double.valueOf(linkId)*Double.valueOf(linkLength);
				double qStartTime =Double.valueOf(queuingTime);
				double qStartDistFromFNode = initialPos+(qStartTime-Double.valueOf(linkEnterTime))*vehicleSpeed;
				if((qStartDistFromFNode-initialPos) > Double.valueOf(linkLength)){
					qStartDistFromFNode=initialPos + Double.valueOf(linkLength);
				}
				double timeStepTillFreeSpeed = qStartTime;
				double endOfLink = (1+Double.valueOf(linkId))*Double.valueOf(linkLength);

				// first line will write the distance and time for which speed was free flow speed.
				// next line will write the queue distance and link leave time.
				writer.write(personId+"\t"+linkId+"\t"+linkEnterTime+"\t"+initialPos+"\t"+timeStepTillFreeSpeed+"\t"+qStartDistFromFNode+"\t"+travelMode+"\n");
				writer.write(personId+"\t"+linkId+"\t"+timeStepTillFreeSpeed+"\t"+qStartDistFromFNode+"\t"+(Double.valueOf(linkLeaveTime))+"\t"+endOfLink+"\t"+travelMode+"\n");
				String timeDataLine=personId+"\t"+linkId+"\t"+linkEnterTime+"\t"+linkLeaveTime+"\t"+linkLength+"\t"+travelMode;
				copyLinkEnterLeaveTimeData.remove(timeDataLine);
			}

			for(String timeDataLine : copyLinkEnterLeaveTimeData){
				String timeParts[] = timeDataLine.split("\t");
				String personId = timeParts[0];
				String linkId = timeParts[1];
				String linkEnterTime = timeParts[2];
				String linkLeaveTime = timeParts[3];
				String linkLength = timeParts[4];
				String travelMode = timeParts[5];

				double initialPos = Double.valueOf(linkId)*Double.valueOf(linkLength);
				double finalPos = (1+Double.valueOf(linkId))*Double.valueOf(linkLength);
				writer.write(personId+"\t"+linkId+"\t"+linkEnterTime+"\t"+initialPos+"\t"+linkLeaveTime+"\t"+finalPos+"\t"+travelMode+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written in file. Reason : "+e);
		}
	}

	private static double getVehicleSpeed(String travelMode){
		double vehicleSpeed =0;
		if(travelMode.equals("cars") || travelMode.equals("fast")) { //fast
			vehicleSpeed= 16.67;
		} else if(travelMode.equals("motorbikes") || travelMode.equals("med")) {
			vehicleSpeed = 16.67;
		} else if(travelMode.equals("bicycles") || travelMode.equals("truck") ){
			vehicleSpeed= 4.17;
		}
		return vehicleSpeed;
	}

	private static void writeLinkEnterLeaveTimeForR(){

		List<String> linkEnterLeaveTimeDataList = calculationHandler.getPersonLinkEnterLeaveTimeDataToWrite();
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/rDataPersonLinkEnterLeave.txt");
		try {
			writer.write("personId \t linkId \t linkEnterTimeX1 \t initialPositionY1 \t linkLeaveTimeX2 \t endPositionY2 \t travelMode \n");
			for(String timeDataLine : linkEnterLeaveTimeDataList){
				String timeParts[] = timeDataLine.split("\t");
				String personId = timeParts[0];
				String linkId = timeParts[1];
				String linkEnterTime = timeParts[2];
				String linkLeaveTime = timeParts[3];
				String linkLength = timeParts[4];
				String travelMode = timeParts[5];

				double initialPos = Double.valueOf(linkId)*Double.valueOf(linkLength);
				double finalPos = (1+Double.valueOf(linkId))*Double.valueOf(linkLength);
				writer.write(personId+"\t"+linkId+"\t"+linkEnterTime+"\t"+initialPos+"\t"+linkLeaveTime+"\t"+finalPos+"\t"+travelMode+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written in file. Reason : "+e);
		}
	}
}

