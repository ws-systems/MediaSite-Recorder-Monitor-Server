package edu.sdsu.its.API.Models;

import edu.sdsu.its.Mediasite.Recorders;
import lombok.Getter;

import java.sql.Timestamp;

/**
 * @author Tom Paulus
 *         Created on 5/10/17.
 */
public class Recorder extends Recorders.Recorder {
    @Getter private Status status;
    @Getter private Timestamp lastSeen;

    public Recorder(String id, String name, String description, String serialNumber, String version, String lastVersionUpdateDate, String physicalAddress, String imageVersion, Status status, Timestamp lastSeen) {
        super(id, name, description, serialNumber, version, lastVersionUpdateDate, physicalAddress, imageVersion);
        this.status = status;
        this.lastSeen = lastSeen;
    }

    public Recorder(String id, Status status) {
        super(id);
        this.status = status;
    }
}
