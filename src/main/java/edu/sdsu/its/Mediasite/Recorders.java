package edu.sdsu.its.Mediasite;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.Status;
import edu.sdsu.its.DB;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tom Paulus
 * Created on 5/5/17.
 */
public class Recorders {
    private static final Logger LOGGER = Logger.getLogger(Recorders.class);

    private static final String msPass = DB.getPreference("ms.api-pass");
    private static final String msUser = DB.getPreference("ms.api-user");
    private static final String msAPI = DB.getPreference("ms.api-key");
    private static String msURL = DB.getPreference("ms.url");

    private static final String RECORDER_WEB_SERVICE_PORT = "8090";

    public static Recorder[] getRecorders() {
        final List<Recorder> recorderList = new ArrayList<>();
        msURL = msURL.endsWith("/") ? msURL : msURL + '/';
        String nextPageURL = msURL + "Api/v1/Recorders";

        do {
            HttpResponse<com.mashape.unirest.http.JsonNode> recorderRequest;

            try {
                recorderRequest = Unirest
                        .get(nextPageURL)
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
            RecorderResponse response = gson.fromJson(recorderRequest.getBody().toString(), RecorderResponse.class);
            nextPageURL = response.nextLink;
            Collections.addAll(recorderList, response.value);
        } while (nextPageURL != null && !nextPageURL.isEmpty());

        LOGGER.debug(String.format("Got %d recorders from API", recorderList.size()));

        return recorderList.toArray(new Recorder[]{});
    }

    public static String getRecorderIP(final String recorderId) {
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
            if (e.getCause() instanceof ConnectTimeoutException) {
                LOGGER.warn(String.format("Could not connect to Recorder at IP %s - Connection Timeout", recorderIP));
            }

            LOGGER.error("Problem retrieving recorder status from Recorder - IP: " + recorderIP, e);
            return null;
        }

        Gson gson = new Gson();
        RecorderStatusResponse recorderStatus = gson.fromJson(recorderInfoRequest.getBody().substring(
                recorderInfoRequest.getBody().indexOf('{'),
                recorderInfoRequest.getBody().lastIndexOf('}') + 1),
                RecorderStatusResponse.class);

        return Status.getByName(recorderStatus.recorderStateString);
    }

    @SuppressWarnings("unused")
    private static class RecorderResponse {
        @Expose
        private Recorder[] value;
        @SerializedName("odata.nextLink")
        @Expose
        public String nextLink;
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
}