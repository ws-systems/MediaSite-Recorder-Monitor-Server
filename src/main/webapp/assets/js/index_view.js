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
            $RecorderInfo.find('.modal-recorder-last-seen').text(recorder.lastSeen);

            $RecorderInfo.modal('show');
        },
        error: function (e) {
            if (e.status === 401) {
                // Redirect when login is required
                window.location.replace("login");
            } else {
                console.warn(e);
                sweetAlert("Oops...", "Something went wrong!", "error");
            }
        }
    });
}