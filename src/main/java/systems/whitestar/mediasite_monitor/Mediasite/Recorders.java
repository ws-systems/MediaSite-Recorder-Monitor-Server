package systems.whitestar.mediasite_monitor.Mediasite;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.log4j.Log4j;
import org.apache.http.conn.ConnectTimeoutException;
import systems.whitestar.mediasite_monitor.API.Models.Recorder;
import systems.whitestar.mediasite_monitor.API.Models.Status;
import systems.whitestar.mediasite_monitor.DB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Tom Paulus
 * Created on 5/5/17.
 */
@Log4j
public class Recorders {
    private static final String msPass = DB.getPreference("ms.api-pass");
    private static final String msUser = DB.getPreference("ms.api-user");
    private static final String msAPI = DB.getPreference("ms.api-key");
    private static final String RECORDER_WEB_SERVICE_PORT = "8090";
    private static String msURL = DB.getPreference("ms.url");

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
                log.error("Problem retrieving recorder list from MS API", e);
                return null;
            }

            if (recorderRequest.getStatus() != 200) {
                log.error(String.format("Problem retrieving recorder list from MS API. HTTP Status: %d",
                        recorderRequest.getStatus()));
                log.info(recorderRequest.getBody());
                return null;
            }

            Gson gson = new Gson();
            //noinspection unchecked
            RecorderResponse response = gson.fromJson(recorderRequest.getBody().toString(), RecorderResponse.class);
            nextPageURL = response.nextLink;
            Collections.addAll(recorderList, response.value);
        } while (nextPageURL != null && !nextPageURL.isEmpty());

        log.debug(String.format("Got %d recorders from API", recorderList.size()));

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
            log.error("Problem retrieving recorder info from MS API - ID: " + recorderId, e);
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
                log.warn(String.format("Could not connect to Recorder at IP %s - Connection Timeout", recorderIP));
            }

            log.error("Problem retrieving recorder status from Recorder - IP: " + recorderIP, e);
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
        @SerializedName("odata.nextLink")
        @Expose
        public String nextLink;
        @Expose
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
}