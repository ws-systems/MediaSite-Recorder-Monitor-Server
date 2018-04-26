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
    $modal.find('#updateName').val($user.data("name"));
    $modal.find('#updateNotifyCheckbox').prop('checked', $user.data("notify"));

    if ($('#users').find('tr').length === 2) { // Header Row + User Rows
        // Only One User - Prevent Deletion
        $('#deleteUserBtn').prop("disabled", true);
    } else {
        $('#deleteUserBtn').prop("disabled", false);
    }

    $modal.modal('show');
}

function updateUser() {
    const $updateEmail = $('#updateEmail');

    var payload = {};
    payload.name = $('#updateName').val();
    payload.email = $updateEmail.val();
    payload.notify = $('#updateNotifyCheckbox')[0].checked;

    $.ajax({
        method: "PUT",
        url: "/api/users/" + $updateEmail.val(),
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function (req) {
            var $row = $('#user-' + req.PK);
            $row.data('name', req.name);
            $row.data('email', req.email);
            $row.data('notify', req.notify);

            $row.find('td:nth-child(1)').innerHTML = req.name;
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