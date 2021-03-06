function updateSettings() {
    $.LoadingOverlay("show", {
        image       : "",
        fontawesome : "fas fa-spinner fa-spin"
    });

    var payload = [];
    var $mutableSettings = $('.mutable-setting');

    for (var i = 0; i < $mutableSettings.length; i++) {
        var $settingInput = $($mutableSettings[i]);
        if ($settingInput.val() !== null && $settingInput.val() !== "") {
            payload.push({
                "setting": $settingInput.attr('id'),
                "value": $settingInput.val().toString()
            });
        }
    }

    $.ajax({
        method: "PUT",
        url: "/api/integrations/",
        data: JSON.stringify(payload),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    })
        .done(function (req) {
            var loadDelay = 5000;

            swal({
                title: "Settings Updated!",
                text: "Please wait a moment while they are being applied.<br>" +
                "This page will refresh automatically when done.",
                timer: loadDelay,
                html: true,
                showConfirmButton: false
            });

            setTimeout(function () {
                location.reload(true);
            }, loadDelay);
        })
        .fail(function (req) {
            sweetAlert("Oops...", "Something went wrong!", "error");
            console.log(req);
        })
        .always(function () {
            $.LoadingOverlay("hide", true);
        })
}