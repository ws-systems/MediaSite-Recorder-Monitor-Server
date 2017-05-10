package edu.sdsu.its.API.Models;

import edu.sdsu.its.Mediasite.Recorders;

import java.sql.Timestamp;

/**
 * @author Tom Paulus
 *         Created on 5/10/17.
 */
public class Recorder extends Recorders.Recorder {
    Boolean Online;
    Timestamp LastSeen;

    public Recorder(String id, String name, String description, String serialNumber, String version,
                    String lastVersionUpdateDate, String physicalAddress, String imageVersion,
                    Boolean online, Timestamp lastSeen) {
        super(id, name, description, serialNumber, version, lastVersionUpdateDate, physicalAddress, imageVersion);
        this.Online = online;
        this.LastSeen = lastSeen;
    }
}
