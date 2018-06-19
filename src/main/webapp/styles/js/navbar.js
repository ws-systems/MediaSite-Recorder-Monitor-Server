function updateOwnInfo() {
    var payload = {};
    payload.notify = $('#update_selfNotifyCheckbox')[0].checked;

    $.ajax({
        method: "PUT",
        url: "/api/self/profile",
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function (req) {
            swal("Okay!", "Your info has been updated!", "success");
            var $updateUserModal = $('#updateSelfModal');
            $updateUserModal.find('form')[0].reset();
            $updateUserModal.modal('hide');
        })
        .fail(function (req) {
            if (req.status === 412) {
                // Duplicate Email
                $('#update_selfEmail').parent().addClass("has-danger");
                $('#update_selfDuplicateEmailWarning').show();
            } else {
                sweetAlert("Oops...", "Something went wrong!", "error");
                console.log(req);
                $('#update_selfUserModal').modal('hide');
            }
        });
}

function subscribeToNotifications() {
    $.ajax({
        method: "POST",
        url: "/api/self/subscribe",
        contentType: "application/json; charset=utf-8"
    })
        .done(function () {
            swal("Done!", "Your have been subscribed!", "success");
            $('#welcomeModal').modal('hide');
        })
        .fail(function (req) {
            sweetAlert("Oops...", "Something went wrong!", "error");
            console.log(req);
            $('#welcomeModal').modal('hide');
        });

    return false;
}