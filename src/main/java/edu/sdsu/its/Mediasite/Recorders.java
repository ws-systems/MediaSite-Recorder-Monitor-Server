package edu.sdsu.its.Mediasite;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.sdsu.its.DB;
import org.apache.log4j.Logger;

/**
 * @author Tom Paulus
 *         Created on 5/5/17.
 */
public class Recorders {
    private static final Logger LOGGER = Logger.getLogger(Recorders.class);

    private static final String msPass = DB.getPreference("ms-api-pass");
    private static final String msUser = DB.getPreference("ms-api-user");
    private static final String msAPI = DB.getPreference("ms-api-key");

    public static Recorder[] getRecorders() {
        String msURL = DB.getPreference("ms-url");
        msURL = msURL.endsWith("/") ? msURL : msURL + '/';
        HttpResponse<com.mashape.unirest.http.JsonNode> recorderRequest = null;

        try {
            recorderRequest = Unirest
                    .get(msURL + "api/v1/Recorders")
                    .header("sfapikey", msAPI)
                    .basicAuth(msUser, msPass)
                    .asJson();
        } catch (UnirestException e) {
            LOGGER.error("Problem retrieving recorder list from MS API", e);
            return null;
        }

        if (recorderRequest.getStatus() != 200) {
            LOGGER.error(String.format("Problem retrieving recorder list from MS API. HTTP Status: %d", recorderRequest.getStatus()));
            LOGGER.info(recorderRequest.getBody());
            return null;
        }

        Gson gson = new Gson();
        //noinspection unchecked
        Recorder[] recorders = gson.fromJson(recorderRequest.getBody().toString(), RecorderResponse.class).value;
        LOGGER.debug(String.format("Got %d recorders from API", recorders.length));

        return recorders;
    }

    public static String getRecorderStatus(final String recorderId) {
        String msURL = DB.getPreference("ms-url");
        msURL = msURL.endsWith("/") ? msURL : msURL + '/';
        HttpResponse<com.mashape.unirest.http.JsonNode> recorderStatusRequest = null;

        try {
            recorderStatusRequest = Unirest
                    .get(msURL + "api/v1/Recorders('" + recorderId + "')/Status")
                    .header("sfapikey", msAPI)
                    .basicAuth(msUser, msPass)
                    .asJson();
        } catch (UnirestException e) {
            LOGGER.error("Problem retrieving recorder status from MS API - ID: " + recorderId, e);
            return null;
        }

        if (recorderStatusRequest.getStatus() != 200) {
            LOGGER.error(String.format("Problem retrieving recorder status from MS API. HTTP Status: %d", recorderStatusRequest.getStatus()));
            LOGGER.info(recorderStatusRequest.getBody());
            return null;
        }

        Gson gson = new Gson();
        //noinspection unchecked
        RecorderStatusResponse statusResponse = gson.fromJson(recorderStatusRequest.getBody().toString(), RecorderStatusResponse.class);
        return statusResponse.getRecorderState();
    }

    private static class RecorderResponse {
        Recorder[] value;
    }

    public static class Recorder {
        String Id;
        String Name;
        String Description;
        String SerialNumber;
        String Version;
        String LastVersionUpdateDate;
        String PhysicalAddress;
        String ImageVersion;

        @Override
        public String toString() {
            return "Recorder{" +
                    "Id='" + Id + '\'' +
                    ", Name='" + Name + '\'' +
                    ", Description='" + Description + '\'' +
                    ", SerialNumber='" + SerialNumber + '\'' +
                    ", Version='" + Version + '\'' +
                    ", LastVersionUpdateDate='" + LastVersionUpdateDate + '\'' +
                    ", PhysicalAddress='" + PhysicalAddress + '\'' +
                    ", ImageVersion='" + ImageVersion + '\'' +
                    '}';
        }

        public Recorder(String id, String name, String description, String serialNumber, String version,
                        String lastVersionUpdateDate, String physicalAddress, String imageVersion) {
            Id = id;
            Name = name;
            Description = description;
            SerialNumber = serialNumber;
            Version = version;
            LastVersionUpdateDate = lastVersionUpdateDate;
            PhysicalAddress = physicalAddress;
            ImageVersion = imageVersion;
        }

        public String getId() {
            return Id;
        }

        public String getName() {
            return Name;
        }

        public String getDescription() {
            return Description != null ? Description : "";
        }

        public String getSerialNumber() {
            return SerialNumber;
        }

        public String getVersion() {
            return Version;
        }

        public String getLastVersionUpdateDate() {
            return LastVersionUpdateDate;
        }

        public String getPhysicalAddress() {
            return PhysicalAddress;
        }

        public String getImageVersion() {
            return ImageVersion;
        }
    }

    private static final class RecorderStatusResponse {
        String RecorderState;

        public String getRecorderState() {
            return RecorderState;
        }
    }
}
