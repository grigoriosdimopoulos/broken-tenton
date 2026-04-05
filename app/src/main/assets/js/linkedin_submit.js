(function() {
  try {
    var modal = document.querySelector('.jobs-easy-apply-modal, .jobs-apply-modal') || document;

    // Fill a specific field by elementId
    var fillTarget = '{{FILL_ID}}';
    var fillValue = '{{FILL_VALUE}}';

    if (fillTarget && fillTarget !== '{{FILL_ID}}') {
      var el = modal.querySelector('#' + fillTarget) ||
               modal.querySelector('[name="' + fillTarget + '"]') ||
               modal.querySelector('[id="' + fillTarget + '"]');
      if (el) {
        el.focus();
        el.value = fillValue;
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
        AndroidBridge.onResult('fill_field', JSON.stringify({filled: fillTarget}));
        return;
      }
    }

    // Try to click next / submit
    var action = '{{ACTION}}';
    if (action === 'next') {
      var nextBtn = modal.querySelector(
        'button[aria-label*="Continue"], button[aria-label*="Next"], button[aria-label*="Review"]'
      );
      if (nextBtn) { nextBtn.click(); AndroidBridge.onResult('submit', 'next_clicked'); return; }
    }
    if (action === 'submit' || !action || action === '{{ACTION}}') {
      var submitBtn = modal.querySelector(
        'button[aria-label*="Submit application"], button[aria-label*="Submit"]'
      );
      if (submitBtn) { submitBtn.click(); AndroidBridge.onResult('submit', 'submitted'); return; }
    }

    AndroidBridge.onError('submit', 'No actionable button found');
  } catch(e) {
    AndroidBridge.onError('submit', e.message);
  }
})();
