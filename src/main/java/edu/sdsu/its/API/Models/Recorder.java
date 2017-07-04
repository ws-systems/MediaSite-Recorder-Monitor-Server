package edu.sdsu.its.API.Models;

import edu.sdsu.its.Mediasite.Recorders;
import lombok.Getter;

import java.sql.Timestamp;

/**
 * @author Tom Paulus
 *         Created on 5/10/17.
 */
public class Recorder extends Recorders.Recorder {
    @Getter private String Status;
    @Getter private Timestamp LastSeen;

    public Recorder(String id, String name, String description, String serialNumber, String version, String lastVersionUpdateDate, String physicalAddress, String imageVersion, String status, Timestamp lastSeen) {
        super(id, name, description, serialNumber, version, lastVersionUpdateDate, physicalAddress, imageVersion);
        Status = status;
        LastSeen = lastSeen;
    }
}
