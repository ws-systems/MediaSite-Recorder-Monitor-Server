package edu.sdsu.its.Mediasite;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.sdsu.its.API.Models.Status;
import edu.sdsu.its.DB;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tom Paulus
 *         Created on 5/5/17.
 */
public class Recorders {
    private static final Logger LOGGER = Logger.getLogger(Recorders.class);

    private static final String msPass = DB.getPreference("ms-api-pass");
    private static final String msUser = DB.getPreference("ms-api-user");
    private static final String msAPI = DB.getPreference("ms-api-key");

    private static final String RECORDER_WEB_SERVICE_PORT = "8090";

    public static Recorder[] getRecorders() {
        String msURL = DB.getPreference("ms-url");
        msURL = msURL.endsWith("/") ? msURL : msURL + '/';
        HttpResponse<com.mashape.unirest.http.JsonNode> recorderRequest = null;

        try {
            recorderRequest = Unirest
                    .get(msURL + "Api/v1/Recorders")
                    .header("sfapikey", msAPI)
                    .basicAuth(msUser, msPass)
                    .asJson();
        } catch (UnirestException e) {
            LOGGER.error("Problem retrieving recorder list from MS API", e);
            return null;
        }

        if (recorderRequest.getStatus() != 200) {
            LOGGER.error(String.format("Problem retrieving recorder list from MS API. HTTP Status: %d",
                    recorderRequest.getStatus()));
            LOGGER.info(recorderRequest.getBody());
            return null;
        }

        Gson gson = new Gson();
        //noinspection unchecked
        Recorder[] recorders = gson.fromJson(recorderRequest.getBody().toString(), RecorderResponse.class).value;
        LOGGER.debug(String.format("Got %d recorders from API", recorders.length));

        return recorders;
    }

    public static String getRecorderIP(final String recorderId) {
        String msURL = DB.getPreference("ms-url");
        msURL = msURL.endsWith("/") ? msURL : msURL + '/';
        HttpResponse<String> recorderInfoRequest;

        try {
            recorderInfoRequest = Unirest
                    .get(msURL + "Api/v1/Recorders('" + recorderId + "')")
                    .header("sfapikey", msAPI)
                    .basicAuth(msUser, msPass)
                    .asString();
        } catch (UnirestException e) {
            LOGGER.error("Problem retrieving recorder info from MS API - ID: " + recorderId, e);
            return null;
        }

        Gson gson = new Gson();
        Recorder recorder = gson.fromJson(recorderInfoRequest.getBody(), Recorder.class);

        return recorder.getIP();
    }

    public static Status getRecorderStatus(final String recorderIP) {
        HttpResponse<String> recorderInfoRequest;

        try {
            recorderInfoRequest = Unirest
                    .get("http://" + recorderIP + ":" +
                            RECORDER_WEB_SERVICE_PORT +
                            "/recorderwebapi/v1/action/service/RecorderStateJson")
                    .header("sfapikey", msAPI)
                    .basicAuth(msUser, msPass)
                    .asString();
        } catch (UnirestException e) {
            LOGGER.error("Problem retrieving recorder status from Recorder - IP: " + recorderIP, e);
            return null;
        }

        Gson gson = new Gson();
        RecorderStatusResponse recorderStatus = gson.fromJson(recorderInfoRequest.getBody().substring(
                recorderInfoRequest.getBody().indexOf('{'),
                recorderInfoRequest.getBody().lastIndexOf('}') + 1),
                RecorderStatusResponse.class);

        return Status.getByCode(recorderStatus.recorderState);
    }

    @SuppressWarnings("unused")
    private static class RecorderResponse {
        private Recorder[] value;
    }

    @SuppressWarnings("unused")
    private static class RecorderStatusResponse {
        @SerializedName("RecorderState")
        private Integer recorderState;

        @SerializedName("RecorderStateString")
        private String recorderStateString;

        @SerializedName("SystemState")
        private Integer systemState;

        @SerializedName("SystemStateString")
        private String systemStateString;

        @SerializedName("RemoteAccessEnabled")
        private Boolean remoteAccessEnabled;

        @SerializedName("RecorderRemoteWebAddress")
        private String recorderRemoteWebAddress;

        @SerializedName("ShellSecurityMode")
        private Integer shellSecurityMode;

        @SerializedName("ShellSecurityModeString")
        private String shellSecurityModeString;

        @SerializedName("IsCertificateVerified")
        private Boolean isCertificateVerified;

        @SerializedName("RecorderTicket")
        private String recorderTicket;

        @SerializedName("RecorderCode")
        private String recorderCode;

        @SerializedName("IsPowerControlEnabled")
        private Boolean isPowerControlEnabled;

        @SerializedName("IsUpdateServiceInstalled")
        private Boolean isUpdateServiceInstalled;

        @SerializedName("IsUpdateServiceRunning")
        private Boolean isUpdateServiceRunning;
    }


    public static @AllArgsConstructor class Recorder {
        @Getter private String Id;
        @Getter private String Name;
        @Getter private String Description;
        @Getter private String SerialNumber;
        @Getter private String Version;
        @Getter private String WebServiceUrl;
        @Getter private String LastVersionUpdateDate;
        @Getter private String PhysicalAddress;
        @Getter private String ImageVersion;

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

        public Recorder(String id, String name, String description, String serialNumber, String version, String lastVersionUpdateDate, String physicalAddress, String imageVersion) {
            Id = id;
            Name = name;
            Description = description;
            SerialNumber = serialNumber;
            Version = version;
            LastVersionUpdateDate = lastVersionUpdateDate;
            PhysicalAddress = physicalAddress;
            ImageVersion = imageVersion;
        }

        String getIP() throws RuntimeException {
            if (this.getWebServiceUrl() == null || this.getWebServiceUrl().isEmpty()) {
                throw new RuntimeException("WebService URL not Defined");
            }

            Pattern pattern = Pattern.compile("\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}");
            Matcher matcher = pattern.matcher(this.getWebServiceUrl());
            if (!matcher.find()) {
                throw new RuntimeException("No IP defined in WebService URL");
            }
            return matcher.group();
        }
    }
}