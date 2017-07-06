function doSubmit() {
    $('[name="password"]').val(btoa($('#inputPassword').val()));
    return true;
}