function showAgentEditModal(elem) {
    var $modal = $('#updateAgentModal');
    var $agent = $($(elem).closest('tr'));

    var rowId = $agent.attr('id');
    $modal.find("#agentID").val(rowId.substring(rowId.indexOf('-') + 1));
    $modal.find('#updateName').val($agent.data("name"));
    $modal.find('#updateAuthorization').prop('checked', $agent.data("authorized"));

    // Prevent deletion of online agents

    var $deleteAgentBtn = $('#deleteAgentBtn');
    if ($agent.find('.agent-status').find('*').hasClass('online')) {
        $deleteAgentBtn.prop("disabled", true);
    } else {
        $deleteAgentBtn.prop("disabled", false);
    }

    $modal.modal('show');
}

function updateAgent() {
    var agentID = $('#agentID').val();

    var payload = {};
    payload.name = $('#updateName').val();
    payload.id = agentID;
    payload.authorized = $('#updateAuthorization')[0].checked;

    $.ajax({
        method: "PUT",
        url: "/api/agents/" + agentID,
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function (req) {
            var loadDelay = 2500;
            $('#updateAgentModal').modal('hide');

            swal({
                title: "Agent Updated!",
                text: "Please wait a moment while your changes are being applied.<br>" +
                "This page will refresh automatically when done.",
                timer: loadDelay,
                html: true,
                showConfirmButton: false,
                icon: "success"
            });

            setTimeout(function () {
                location.reload(true);
            }, loadDelay);
        })
        .fail(function (req) {
            sweetAlert("Oops...", "Something went wrong!", "error");
            console.log(req);
            $('#updateAgentModal').modal('hide');
        });
}


function deleteAgent() {
    var $modal = $('#updateAgentModal');

    var agentID = $modal.find('#agentID').val();
    $.ajax({
        method: "DELETE",
        url: "/api/agents/" + agentID
    })
        .done(function (req) {
            $('#agent-' + agentID).remove();
            swal("Okay!", "Agent has been deleted.", "success");
        })
        .fail(function (req) {
            sweetAlert("Oops...", "Something went wrong!", "error");
            console.log(req);
        })
        .always(function () {
            $modal.modal('hide');
        });
}