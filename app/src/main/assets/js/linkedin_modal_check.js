(function() {
  try {
    var modal = document.querySelector('.jobs-easy-apply-modal, .jobs-apply-modal');
    var errorEls = modal
      ? modal.querySelectorAll(
          '.artdeco-inline-feedback--error, ' +
          '[data-test-form-element-error-message], ' +
          '.fb-form-element__error-text, ' +
          '[class*="error-message"], ' +
          '[role="alert"]'
        )
      : [];

    var errors = [];
    errorEls.forEach(function(el) {
      var t = el.textContent.trim();
      if (t) errors.push(t);
    });

    AndroidBridge.onResult('modal_check', JSON.stringify({
      modalOpen: !!modal,
      hasErrors: errors.length > 0,
      errors: errors
    }));
  } catch(e) {
    AndroidBridge.onError('modal_check', e.message);
  }
})();
