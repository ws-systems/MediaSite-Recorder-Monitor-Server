getRecorders();

function getRecorders() {
    $.ajax({
        type: "GET",
        url: 'api/recorders',
        success: function (payload) {
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
        },
        error: function (e) {
            console.warn(e);
            sweetAlert("Oops...", "Something went wrong!", "error");
        }
    });
}

function showRecorderByID(recorderID) {
    $.ajax({
        type: "GET",
        url: 'api/recorders/' + recorderID,
        success: function (recorder) {
            var $RecorderInfo = $('#RecorderInfo');

            $RecorderInfo.find('span.modal-recorder-id').text(recorder.Id);
            $RecorderInfo.find('.modal-recorder-name').text(recorder.Name);
            $RecorderInfo.find('.modal-recorder-description').text(recorder.Description);
            $RecorderInfo.find('.modal-recorder-serial').text(recorder.SerialNumber);
            $RecorderInfo.find('.modal-recorder-version').text(recorder.Version);
            $RecorderInfo.find('.modal-recorder-updated').text(recorder.LastVersionUpdateDate);
            $RecorderInfo.find('.modal-recorder-address').text(recorder.PhysicalAddress);
            $RecorderInfo.find('.modal-recorder-img-version').text(recorder.ImageVersion);
            $RecorderInfo.find('.modal-recorder-last-seen').text(recorder.LastSeen);

            $RecorderInfo.modal('show');
        },
        error: function (e) {
            console.warn(e);
            sweetAlert("Oops...", "Something went wrong!", "error");
        }
    });
}