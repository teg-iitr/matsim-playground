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
package playground.agarwalamit.siouxFalls.contoler;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;
import playground.ikaddoura.internalizationCar.MarginalCostPricing;
import playground.ikaddoura.internalizationCar.TollDisutilityCalculatorFactory;
import playground.ikaddoura.internalizationCar.TollHandler;
import playground.vsp.emissions.EmissionModule;

/**
 * @author amit
 */
public class SiouxFallsControler {

	public static void main(String[] args) {

		boolean internalizeEmission = false; //run0 false, false false; run1 true, false false; run2 false,true, false; run 3 false, false true
		boolean internalizeCongestion = false;
		boolean both = false;

		String configFile = "../../input/SiouxFalls_config.xml";

		String emissionEfficiencyFactor ="1.0";
		String considerCO2Costs = "true";
		String emissionCostFactor = "1.0";

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory("../../output/run0/");

		//===vsp defaults
//		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
//		config.vspExperimental().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration.toString());
//		config.timeAllocationMutator().setMutationRange(7200.);
//		config.timeAllocationMutator().setAffectingDuration(false);
//		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.ABORT);

		Controler controler = new Controler(config);

		//===emission files
		config.vspExperimental().setAverageColdEmissionFactorsFile("./input/emissionFiles/averageColdEmissionFactorsFile.txt");
		config.vspExperimental().setAverageWarmEmissionFactorsFile("./input/emissionFiles/averageWarmEmissionFactorsFile.txt");
		config.vspExperimental().setEmissionRoadTypeMappingFile("./input/emissionFiles/roadTypeMapping.txt");
		config.vspExperimental().setEmissionVehicleFile("./input/emissionFiles/emissionVehicles.xml");
		config.vspExperimental().setUsingDetailedEmissionCalculation(false);
		//===only emission events genertaion; used with all runs for comparisons
		EmissionModule emissionModule = new EmissionModule(ScenarioUtils.loadScenario(config));
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		if(internalizeEmission)
		{
			//===internalization of emissions
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
			controler.setTravelDisutilityFactory(emissionTducf);
			controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
		}

		if(internalizeCongestion) 
		{
			//=== internalization of congestion
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			controler.addControlerListener(new MarginalCostPricing((ScenarioImpl) controler.getScenario(), tollHandler ));
		}
		
//		if(both) {
//			TollHandler tollHandler = new TollHandler(controler.getScenario());
//			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
//			EmissionCongestionTravelDisutilityCalculatorFactory emissionCongestionTravelDisutilityCalculatorFactory = new EmissionCongestionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, tollHandler);
//			controler.setTravelDisutilityFactory(emissionCongestionTravelDisutilityCalculatorFactory);
//			controler.addControlerListener(new InternalizeEmissionsCongestionControlerListener(emissionModule, emissionCostModule, (ScenarioImpl) controler.getScenario(), tollHandler));
//		}

		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		controler.setDumpDataAtEnd(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.run();	

	}

}
