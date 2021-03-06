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

package playground.amit.opdyts.teleportationModes;

import java.util.LinkedHashSet;
import java.util.Set;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Created by amit on 15.06.17.
 */


public class Zone {

    public Zone (final String id) {
        this.zoneId = Id.create(id, Zone.class);
    }
    private final Id<Zone> zoneId;

    public Id<Zone> getZoneId() {
        return zoneId;
    }

    // link ids inside zone

    /**
     * I think, this should not be used because using a link to identify the zone will be erroneous if link is longer.
     */
    @Deprecated
    public Set<Id<Link>> getLinksInsideZone() {
        return linksInsideZone;
    }

    public void addLinksToZone(final Id<Link> linkId) {
        this.linksInsideZone.add(linkId);
    }

    /**
     * I think, this should not be used because using a link to identify the zone will be erroneous if link is longer.
     */
    @Deprecated
    private Set<Id<Link>> linksInsideZone = new LinkedHashSet<>();

    // coordinates of origin/destination

    private Set<Coord> coordsInsideZone = new LinkedHashSet<>();

    public  Set<Coord> getCoordsInsideZone() {return coordsInsideZone;}

    public void addCoordsToZone(final Coord coord) {
        this.coordsInsideZone.add(coord);
    }
}
