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
package playground.agarwalamit.analysis.spatial;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.MathException;
import org.apache.commons.math.special.Erf;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A class to interpolate effect of emissions on each link on other parts of study area.
 * @author amit
 */

public class SpatialInterpolation {

	public SpatialInterpolation() {
		new File(SpatialDataInputs.outputDir).mkdirs();

		SpatialDataInputs.LOG.info("Creating grids inside polygon of bounding box.");
		createGridFromBoundingBox();
		this.grid.writeGrid(SpatialDataInputs.outputDir, SpatialDataInputs.targetCRS.toString());

		// initialize map
		this.cellWeights = new HashMap<Point, Double>();
		for(Point p :this.grid.getGrid().values()){
			this.cellWeights.put(p, 0.);
		}
	}

	private GeometryFactory gf;
	private GeneralGrid grid ;
	public Polygon boundingBoxPolygon;

	private Map<Point, Double> cellWeights;

	/**
	 * @return general grid of cells from pre defined bounding box
	 */
	private void createGridFromBoundingBox (){
		gf = new GeometryFactory();
		// create polygon from bounding box and then get polygon from that
		Coordinate c1 = new Coordinate(SpatialDataInputs.xMin,SpatialDataInputs.yMin);
		Coordinate c2 = new Coordinate(SpatialDataInputs.xMax,SpatialDataInputs.yMin);
		Coordinate c3 = new Coordinate(SpatialDataInputs.xMin,SpatialDataInputs.yMax);
		Coordinate c4 = new Coordinate(SpatialDataInputs.xMax,SpatialDataInputs.yMax);

		boundingBoxPolygon = gf.createPolygon(new Coordinate []{c1,c2,c4,c3,c1}); // sequence to create polygon is equally important

		this.grid = new GeneralGrid(SpatialDataInputs.cellWidth, SpatialDataInputs.gridType);
		this.grid.generateGrid(boundingBoxPolygon);

		SpatialDataInputs.LOG.info("Total number of cells in the grid are "+this.grid.getGrid().size());
	}

	/**
	 * @param link
	 * @param intensityOnLink (emissions etc) intensity for 100% sample.
	 */
	public void processLink(Link link, double intensityOnLink){

		Coordinate linkCentroid = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
		Coordinate fromNodeCoord = new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY());
		Coordinate toNodeCoord = new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY());
		Point linkcentroidPoint = gf.createPoint(linkCentroid);

		if(!boundingBoxPolygon.covers(linkcentroidPoint)) return;

		for(Point p: this.cellWeights.keySet()){

			Coordinate pointCoord = p.getCoordinate();

			double cellArea = this.grid.getCellGeometry(p).getArea();
			double area_smoothingCircle = Math.PI * SpatialDataInputs.smoothingRadius *SpatialDataInputs.smoothingRadius;
			double normalizationFactor = cellArea/area_smoothingCircle;
			double weightSoFar = this.cellWeights.get(p);
			double weightNow;


			if(SpatialDataInputs.linkWeigthMethod.equals("line")){

				weightNow = intensityOnLink * calculateWeightFromLine(fromNodeCoord,toNodeCoord,pointCoord) * normalizationFactor;

			} else if(SpatialDataInputs.linkWeigthMethod.equals("point")) {

				weightNow = intensityOnLink * calculateWeightFromPoint(linkCentroid, pointCoord) * normalizationFactor;

			} else throw new RuntimeException("Averaging method for weight is not recongnized. Use 'line' or 'point'.");

			this.cellWeights.put(p, weightNow+weightSoFar);
		}
	}

	/**
	 * @param fromNodeCoord
	 * @param toNodeCoord
	 * @param cellCentroid
	 * @return The outcome is derived assuming constant emission on link and then integrating effect of emission on link on the cell centroid.
	 */
	private double calculateWeightFromLine(Coordinate fromNodeCoord,Coordinate toNodeCoord, Coordinate cellCentroid){
		double constantA = fromNodeCoord.distance(cellCentroid) * fromNodeCoord.distance(cellCentroid);
		double constantB = (toNodeCoord.x-fromNodeCoord.x)*(fromNodeCoord.x-cellCentroid.x) + 
				(toNodeCoord.y-fromNodeCoord.y)*(fromNodeCoord.y-cellCentroid.y);
		double constantC = 0.;
		double linkLengthFromCoord = fromNodeCoord.distance(toNodeCoord);
		double constantR = SpatialDataInputs.smoothingRadius;

		constantC= (constantR*(Math.sqrt(Math.PI))/(linkLengthFromCoord*2))*Math.exp(-(constantA-(constantB*constantB/(linkLengthFromCoord*linkLengthFromCoord)))/(constantR*constantR));

		double upperLimit = (linkLengthFromCoord+(constantB/linkLengthFromCoord));
		double lowerLimit = constantB/(linkLengthFromCoord);
		double integrationUpperLimit;
		double integrationLowerLimit;
		try {
			integrationUpperLimit = Erf.erf(upperLimit/constantR);
			integrationLowerLimit = Erf.erf(lowerLimit/constantR);
		} catch (MathException e) {
			throw new RuntimeException("Error function is not defined for " + upperLimit + " or " + lowerLimit + "; Exception: " + e);
		}
		double  weight = constantC *(integrationUpperLimit-integrationLowerLimit);

		if(weight<0.0) {
			throw new RuntimeException("Weight is negative, eeight = "+weight+ ". Thus aborting.");
		}
		return weight;
	}


	private double calculateWeightFromPoint(Coordinate linkCentroid, Coordinate cellCentroid){
		double dist = linkCentroid.distance(cellCentroid);
		double smoothingRadius_square = SpatialDataInputs.smoothingRadius * SpatialDataInputs.smoothingRadius;
		double weight = Math.exp((- dist * dist) / (smoothingRadius_square));
		return weight;
	}

	/**
	 * Point (key of map) could be centroid of hexagonal grid or square grid
	 */
	public Map<Point, Double> getCellWeights(){
		return this.cellWeights;
	}

	public void writeSurfacePlotRData(){
		SpatialDataInputs.LOG.info("====Writing data to plot polygon surface in R.====");

		GridType type = SpatialDataInputs.gridType;
		String fileName = SpatialDataInputs.outputDir+"rSurfacePlot"+"_"+type+"_"+SpatialDataInputs.linkWeigthMethod+".txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);

		int noOfSidesOfPolygon = 0;
		if(type.equals(GridType.SQUARE)) noOfSidesOfPolygon = 4;
		else if(type.equals(GridType.HEX)) noOfSidesOfPolygon = 6;
		else throw new RuntimeException(type +" is not a valid grid type.");

		try {
			for(int i=0;i<noOfSidesOfPolygon;i++){
				writer.write("polyX"+i+"\t"+"polyY"+i+"\t");
			}
			writer.write("centroidX \t centroidY \t weight \n ");

			for(Point p : this.cellWeights.keySet()){
				Geometry g = this.grid.getCellGeometry(p);
				Coordinate[] ca = g.getCoordinates();
				for(int i = 0; i < ca.length-1; i++){ // a square polygon have 5 coordinate, first and last is same. 
					writer.write(ca[i].x+"\t"+ca[i].y+"\t");
				}
				double thisWeight = this.cellWeights.get(p);
				writer.write(p.getX()+"\t"+p.getY()+"\t"+thisWeight+"\n");
			}
			writer.close();
			SpatialDataInputs.LOG.info("Data is written to file "+fileName);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
	}

	public void writeRContourPlotData(){
		SpatialDataInputs.LOG.info("====Writing data to plot filled contour in R.====");

		GridType type = SpatialDataInputs.gridType;
		String fileName = SpatialDataInputs.outputDir+"rContour_plot"+"_"+type+"_"+SpatialDataInputs.linkWeigthMethod+".txt";
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
			List<Double> xCoords = new ArrayList<Double>();
			List<Double> yCoords = new ArrayList<Double>();

			for(Point p : this.cellWeights.keySet()){
				if(this.cellWeights.get(p)>0.){
					xCoords.add(p.getX());
					yCoords.add(p.getY());
				}
			}
			Collections.sort(xCoords);
			Collections.sort(yCoords);

			writer.write("\t");
			//x-coordinates as first row
			for(int xIndex = 0; xIndex < xCoords.size(); xIndex++){
				writer.write(xCoords.get(xIndex).toString() + "\t");
			}
			writer.newLine();
			for(int yIndex = 0; yIndex < yCoords.size(); yIndex++){
				//y-coordinates as first column
				writer.write(yCoords.get(yIndex) + "\t");
				for(int xIndex =0; xIndex < xCoords.size(); xIndex++){
					Point p = gf.createPoint(new Coordinate(xCoords.get(xIndex), yCoords.get(yIndex)));
					double emissionWeight = this.cellWeights.get(p);
					writer.write(Double.toString(emissionWeight)+"\t");
				}
				writer.newLine();
				SpatialDataInputs.LOG.info("Writing line "+yIndex);
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
		SpatialDataInputs.LOG.info("Data is written to file "+fileName);
	}

	public boolean isInResearchArea(Link link) {
		Coordinate linkCentroid = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
		Point linkcentroidPoint = gf.createPoint(linkCentroid);

		return boundingBoxPolygon.covers(linkcentroidPoint);
	}

}