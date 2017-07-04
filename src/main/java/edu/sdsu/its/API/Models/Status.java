package edu.sdsu.its.API.Models;

import lombok.Getter;

/**
 * @author Tom Paulus
 *         Created on 7/3/17.
 */
public enum Status {
    UNAVAILABLE(0, "Unavailable"),
    IDLE(1, "Idle"),
    BUSY(2, "Busy"),
    RECORDING(3, "Recording"),
    PRE_RECORD(4, "Before Record"),
    POST_RECORD(5, "AfterRecord"),
    PAUSED(6, "Paused"),
    INCOMPATIBLE(7, "Incompatible"),
    ERROR(8, "Unknown Error"),
    ERROR_INFO(9, "Error Additional Info"),
    DETAILS_DEVICE(10, "Details Devices"),
    MONITOR(11, "Monitor"),
    COULD_NOT_DELETE_ALL_FORMAT(12, "Could Not Delete All Format"),
    COULD_NOT_LICENSE(13, "Could Not License"),
    LICENSE_INITIATED(14, "License Initiated"),
    UPDATE_LOGIN_ERROR(15, "Update Login Error"),
    UPDATE_ERROR(16, "Update Error"),
    UPDATE_SUCCESS(17, "Update Success"),
    UPDATE_REMOVE(18, "Update Remove");

    private @Getter int stateCode;
    private @Getter String stateString;

    Status(int stateCode, String stateString) {
        this.stateCode = stateCode;
        this.stateString = stateString;
    }

    public static Status getByCode(int statusCode) {
        for (Status s: values()) {
            if (s.stateCode == statusCode) {
                return s;
            }
        }
        return null;
    }
}
