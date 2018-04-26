const refreshRate = 2500; // Check that the Stream is Alive Every X Milliseconds

var SSE_source = null;
var processedEvents = [];

$(document).ready(function () {
    loadEvents();
    window.setInterval(checkStream, refreshRate);
});

/**
 * Load Events via Server Sent Events
 */
function loadEvents() {
    SSE_source = new EventSource("/api/stream");
    SSE_source.onmessage = function (event) {
        var obj = JSON.parse(event.data);
        console.trace(obj);

        if (!processedEvents.includes(obj.id)) {
            processedEvents.push(obj.id);
            const $recorderTable = $("#recorder-table");
            var $row;

            switch (obj.cause) {
                case "RECORDER_STATUS_UPDATE":
                    $row = $('#recorder-' + obj.recorder.Id);
                    $row.children('.recorder-status').removeClass(function (index, className) {
                        return (className.match(/(^|\s)status-\S+/g) || []).join(' ');
                    });

                    $row.children('.recorder-status').addClass("status-" + obj.recorder.status.code);
                    $row.children('.recorder-status').text(obj.recorder.status.string);
                    $row.children('.recorder-name').text(obj.recorder.Name);
                    $row.children('.recorder-version').text(obj.recorder.Version);
                    $row.children('.recorder-last-seen').text(obj.recorder.lastSeen);
                    break;

                case "RECORDER_RECORD_UPDATE":
                    if ($recorderTable.has("#recorder-" + obj.recorder.Id).length) {
                        // Recorder already in Table, Skipping
                        break;
                    }

                    var row = $recorderTable[0].insertRow(0);
                    $row = $(row);

                    $row.addClass("recorder")
                        .addClass("recorder-" + obj.recorder.Id)
                        .attr("id", "recorder-" + obj.recorder.Id);

                    $(row.insertCell(0))
                        .addClass("recorder-status")
                        .addClass("status-" + obj.recorder.status.code)
                        .text(obj.recorder.status.string);

                    $(row.insertCell(1))
                        .addClass("recorder-name")
                        .text(obj.recorder.Name);

                    $(row.insertCell(2))
                        .addClass("recorder-version")
                        .text(obj.recorder.Verison);

                    $(row.insertCell(3))
                        .addClass("recorder-last-seen")
                        .text(obj.recorder.lastSeen);

                    $(row.insertCell(4))
                        .addClass("recorder-more-info-button")
                        .addClass("recorder-info")
                        .html("<i class=\"fa fa-info-circle\" aria-hidden=\"true\"\n" +
                            "                           onclick=\"showRecorderByID('" + obj.recorder.Id + "')\"></i>");

                    break;

                case "RECORDER_ALARM_ACTIVATE":
                    // TODO
                    break;

                case "RECORDER_ALARM_CLEAR":
                    // TODO
                    break;

                default:
                    console.warn("Unrecognized Event Type!");
            }

            console.debug("Processed SSE Event");
            sorttable.makeSortable($recorderTable[0])
        } else {
            console.debug("Skipping Event - Already Processed")
        }
    };

    SSE_source.onError = function () {
        loadEvents();
    };
}

/**
 * Check if the Stream is closed, and if so, recreate and open it.
 */
function checkStream() {
    if (SSE_source.readyState === 2) {  // CLOSED == 2
        loadEvents();
    }
}