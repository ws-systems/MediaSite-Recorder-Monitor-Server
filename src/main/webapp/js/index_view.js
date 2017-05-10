getRecorders();

function getRecorders() {
    $.ajax({
        type: "GET",
        url: 'api/recorders',
        success: showRecorders,
        error: function(e) {
            console.warn(e);
            sweetAlert("Oops...", "Something went wrong!", "error");
        }
    });
}

function showRecorders(payload) {
    var table = $('#recorder-table').find('tbody')[0];

    for (var r = 0; r < payload.length; r++) {
        var recorder = payload[r];
        var row = table.insertRow();
        row.insertCell(0).className = recorder.Online ? 'status-ok' : 'status-bad';
        row.insertCell(1).innerHTML = recorder.Name;
        row.insertCell(2).innerHTML = recorder.Version;
        row.insertCell(3).innerHTML = recorder.LastSeen;
        var infoCell = row.insertCell(4);
        infoCell.className = 'recorder-info';
        infoCell.innerHTML = '<i class="fa fa-info-circle" aria-hidden="true" onclick="showRecorderByID(\'' + recorder.Id + '\')"></i>'
    }
}

function showRecorderByID(recorderID) {
    // TODO
}