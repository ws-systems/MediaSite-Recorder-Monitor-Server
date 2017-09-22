function updateOwnInfo() {
    const $updateEmail = $('#update_selfEmail');
    var password = $('#update_selfPassword1').val();

    if (password !== $('#update_selfPassword2').val()) {
        // Password Mismatch
        $('#update_selfPassword1, #update_selfPassword2').val("");
        $('.passwordInput').addClass("has-danger");
        $('.passwordMismatchErr').show();
        return;
    } else {
        $('.passwordInput').removeClass("has-danger");
        $('.passwordMismatchErr').hide();
    }

    $updateEmail.parent().removeClass("has-danger");
    $('#update_selfDuplicateEmailWarning').hide();

    var payload = {};
    payload.firstName = $('#update_selfFirstName').val();
    payload.lastName = $('#update_selfLastName').val();
    payload.email = $updateEmail.val();
    if (password !== "") payload.password = password;
    payload.notify = $('#update_selfNotifyCheckbox')[0].checked;

    $.ajax({
        method: "PUT",
        url: "/api/users/" + $('#update_selfPreviousEmail').val(),
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function (req) {
            swal("Okay!", "Your info has been updated!", "success");
            const $updateUserModal = $('#updateSelfModal');
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