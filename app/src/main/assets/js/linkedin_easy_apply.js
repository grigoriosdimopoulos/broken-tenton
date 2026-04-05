(function() {
  try {
    // Check if Easy Apply modal is open
    var modal = document.querySelector('.jobs-easy-apply-modal, .jobs-apply-modal');

    if (!modal) {
      // Try to click the Easy Apply button to open modal
      var applyBtn = document.querySelector(
        '.jobs-apply-button--top-card, [aria-label*="Easy Apply"], .jobs-s-apply button'
      );
      if (applyBtn) {
        applyBtn.click();
        AndroidBridge.onResult('easy_apply', JSON.stringify({action: 'opened_modal'}));
      } else {
        // Check if there's an external apply button instead
        var externalBtn = document.querySelector('.jobs-apply-button a[href], .apply-button a');
        if (externalBtn) {
          AndroidBridge.onResult('easy_apply', JSON.stringify({action: 'external', url: externalBtn.href}));
        } else {
          AndroidBridge.onError('easy_apply', 'No apply button found');
        }
      }
      return;
    }

    // Modal is open - fill current page fields
    var fields = [];
    var inputs = modal.querySelectorAll('input:not([type=hidden]):not([type=submit]), select, textarea');
    inputs.forEach(function(input) {
      fields.push({
        type: input.tagName.toLowerCase(),
        inputType: input.type || '',
        name: input.name || input.id || '',
        label: findLabel(input),
        value: input.value || ''
      });
    });

    // Check for next/submit button
    var nextBtn = modal.querySelector('button[aria-label*="Continue"], button[aria-label*="Next"], button[aria-label*="Review"]');
    var submitBtn = modal.querySelector('button[aria-label*="Submit application"], button[aria-label*="Submit"]');
    var hasSubmit = !!submitBtn;

    AndroidBridge.onResult('easy_apply', JSON.stringify({
      action: 'form_page',
      fields: fields,
      hasSubmit: hasSubmit
    }));
  } catch(e) {
    AndroidBridge.onError('easy_apply', e.message);
  }

  function findLabel(el) {
    var id = el.id;
    if (id) {
      var lbl = document.querySelector('label[for="' + id + '"]');
      if (lbl) return lbl.textContent.trim();
    }
    var parent = el.closest('.fb-form-element, .artdeco-text-input--container, label');
    if (parent) return parent.textContent.trim().substring(0, 100);
    return '';
  }
})();
