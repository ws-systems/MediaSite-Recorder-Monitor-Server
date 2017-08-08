/**
 * Created by tom on 7/16/17.
 */

/**
 * Pull User info from API and show update modal
 *
 * @param elem DOM Context
 */
function showEditModal(elem) {
    const $modal = $('#updateUserModal');
    const $user = $($(elem).closest('tr'));

    var rowId = $user.attr('id');
    $modal.find("#userPK").val(rowId.substring(rowId.lastIndexOf('-') + 1));
    $modal.find('#updateEmail').val($user.data("email"));
    $modal.find('#updatePreviousEmail').val($user.data("email"));
    $modal.find('#updateFirstName').val($user.data("fname"));
    $modal.find('#updateLastName').val($user.data("lname"));
    $modal.find('#updateNotifyCheckbox').prop('checked', $user.data("notify"));

    if ($('#users').find('tr').length === 2) { // Header Row + User Rows
        // Only One User - Prevent Deletion
        $('#deleteUserBtn').prop("disabled", true);
    } else {
        $('#deleteUserBtn').prop("disabled", false);
    }

    $modal.modal('show');
}

function createUser() {
    const $createEmail = $('#createEmail');
    var password = $('#createPassword1').val();

    if (password !== $('#createPassword2').val()) {
        // Password Mismatch
        $('#createPassword1, #createPassword2').val("");
        $('.passwordInput').addClass("has-danger");
        $('.passwordMismatchErr').show();
        return;
    } else {
        $('.passwordInput').removeClass("has-danger");
        $('.passwordMismatchErr').hide();
    }

    $createEmail.parent().removeClass("has-danger");
    $('#createDuplicateEmailWarning').hide();

    var payload = {};
    payload.firstName = $('#createFirstName').val();
    payload.lastName = $('#createLastName').val();
    payload.email = $createEmail.val();
    payload.password = password;
    payload.notify = $('#createNotifyCheckbox')[0].checked;

    $.ajax({
        method: "POST",
        url: "/api/users",
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function (req) {
            var table = $('#users')[0];
            var row = table.insertRow(-1);
            row.id = "user-" + req.PK;
            $(row).data("email", req.email)
                .data("fname", req.firstName)
                .data("lname", req.lastName)
                .data("notify", req.notify);

            row.insertCell(0).innerHTML = req.firstName;
            row.insertCell(1).innerHTML = req.lastName;
            row.insertCell(2).innerHTML = req.email;

            var notifyStatus = row.insertCell(3);
            notifyStatus.classList += "text-justify";
            if (req.notify) {
                notifyStatus.innerHTML = "<i class=\"fa fa-check\" aria-hidden=\"true\"></i>\n" +
                    "                                <span class=\"sr-only\">Subscribed to notifications</span>";
            } else {
                notifyStatus.innerHTML = " <i class=\"fa fa-times\" aria-hidden=\"true\"></i>\n" +
                    "                                <span class=\"sr-only\">Not subscribed to notifications</span>";
            }

            var editBtn = row.insertCell(4);
            editBtn.classList += "text-right";
            editBtn.innerHTML = "<button type=\"button\"\n" +
                "                                    class=\"btn btn-secondary btn-sm\"\n" +
                "                                    onclick=\"showEditModal(this)\">\n" +
                "                                <i class=\"fa fa-pencil\" aria-hidden=\"true\"></i>&nbsp; Edit User\n" +
                "                            </button>";


            const $createUserModal = $('#createUserModal');
            $createUserModal.find('form')[0].reset();
            $createUserModal.modal('hide');
        })
        .fail(function (req) {
            if (req.status === 412) {
                // Duplicate Email
                $('#createEmail').parent().addClass("has-danger");
                $('#createDuplicateEmailWarning').show();
            } else {
                sweetAlert("Oops...", "Something went wrong!", "error");
                console.log(req);
                $('#createUserModal').modal('hide');

            }
        });
}

function updateUser() {
    const $updateEmail = $('#updateEmail');
    var password = $('#updatePassword1').val();

    if (password !== $('#updatePassword2').val()) {
        // Password Mismatch
        $('#updatePassword1, #updatePassword2').val("");
        $('.passwordInput').addClass("has-danger");
        $('.passwordMismatchErr').show();
        return;
    } else {
        $('.passwordInput').removeClass("has-danger");
        $('.passwordMismatchErr').hide();
    }

    $updateEmail.parent().removeClass("has-danger");
    $('#updateDuplicateEmailWarning').hide();

    var payload = {};
    payload.firstName = $('#updateFirstName').val();
    payload.lastName = $('#updateLastName').val();
    payload.email = $updateEmail.val();
    if (password !== "") payload.password = password;
    payload.notify = $('#updateNotifyCheckbox')[0].checked;

    $.ajax({
        method: "PUT",
        url: "/api/users/" + $('#updatePreviousEmail').val(),
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function (req) {
            var $row = $('#user-' + req.PK);
            $row.data('fname', req.firstName);
            $row.data('lname', req.lastName);
            $row.data('email', req.email);
            $row.data('notify', req.notify);

            $row.find('td:nth-child(1)').innerHTML = req.firstName;
            $row.find('td:nth-child(2)').innerHTML = req.lastName;
            $row.find('td:nth-child(3)').innerHTML = req.email;

            if (req.notify) {
                $row.find('td:nth-child(4)').innerHTML = "<i class=\"fa fa-check\" aria-hidden=\"true\"></i>\n" +
                    "                                <span class=\"sr-only\">Subscribed to notifications</span>";
            } else {
                $row.find('td:nth-child(4)').innerHTML = " <i class=\"fa fa-times\" aria-hidden=\"true\"></i>\n" +
                    "                                <span class=\"sr-only\">Not subscribed to notifications</span>";
            }

            swal("Okay!", "User has been updated.", "success");
            const $updateUserModal = $('#updateUserModal');
            $updateUserModal.find('form')[0].reset();
            $updateUserModal.modal('hide');
        })
        .fail(function (req) {
            if (req.status === 412) {
                // Duplicate Email
                $('#updateEmail').parent().addClass("has-danger");
                $('#updateDuplicateEmailWarning').show();
            } else {
                sweetAlert("Oops...", "Something went wrong!", "error");
                console.log(req);
                $('#updateUserModal').modal('hide');
            }
        });
}

function deleteUser() {
    const $modal = $('#updateUserModal');

    var userEmail = $modal.find('#updateEmail').val();
    $.ajax({
        method: "DELETE",
        url: "/api/users/" + userEmail
    })
        .done(function (req) {
            $('#user-' + $('#userPK').val()).remove();
            swal("Okay!", "User has been deleted.", "success");
        })
        .fail(function (req) {
            sweetAlert("Oops...", "Something went wrong!", "error");
            console.log(req);
        })
        .always(function () {
            $modal.modal('hide');
        });
}