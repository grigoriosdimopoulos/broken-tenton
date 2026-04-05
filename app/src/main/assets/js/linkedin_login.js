(function() {
  try {
    var emailField = document.querySelector('#username, input[name="session_key"], input[type="email"]');
    var passwordField = document.querySelector('#password, input[name="session_password"], input[type="password"]');
    var submitBtn = document.querySelector('[data-litms-control-urn="login-submit"], button[type="submit"], .login__form_action_container button');

    if (!emailField || !passwordField) {
      AndroidBridge.onError('login', 'Login fields not found');
      return;
    }

    // Fill credentials
    emailField.focus();
    emailField.value = '{{EMAIL}}';
    emailField.dispatchEvent(new Event('input', { bubbles: true }));
    emailField.dispatchEvent(new Event('change', { bubbles: true }));

    passwordField.focus();
    passwordField.value = '{{PASSWORD}}';
    passwordField.dispatchEvent(new Event('input', { bubbles: true }));
    passwordField.dispatchEvent(new Event('change', { bubbles: true }));

    if (submitBtn) {
      setTimeout(function() {
        submitBtn.click();
        AndroidBridge.onResult('login', 'submitted');
      }, 500);
    } else {
      AndroidBridge.onError('login', 'Submit button not found');
    }
  } catch(e) {
    AndroidBridge.onError('login', e.message);
  }
})();
