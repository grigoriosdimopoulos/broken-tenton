(function() {
  try {
    // ── 1. Check if Easy Apply modal is already open ─────────────────────────
    var modal =
      document.querySelector('.jobs-easy-apply-modal') ||
      document.querySelector('.jobs-apply-modal') ||
      document.querySelector('[data-test-modal]') ||
      document.querySelector('div[role="dialog"]');

    if (!modal) {
      // ── 2. Try to open it ──────────────────────────────────────────────────
      var applyBtn =
        document.querySelector('button.jobs-apply-button') ||
        document.querySelector('.jobs-apply-button--top-card') ||
        document.querySelector('[aria-label*="Easy Apply"]') ||
        document.querySelector('button[data-control-name*="apply"]') ||
        // 2024/2025 LinkedIn selectors
        document.querySelector('.jobs-s-apply button') ||
        document.querySelector('[data-job-id] ~ div button') ||
        (function() {
          var btns = Array.from(document.querySelectorAll('button'));
          return btns.find(function(b) {
            return /easy apply/i.test(b.textContent) && b.offsetParent !== null;
          });
        })();

      if (applyBtn) {
        applyBtn.click();
        AndroidBridge.onResult('easy_apply', JSON.stringify({ action: 'opened_modal' }));
        return;
      }

      // ── 3. Check for external / non-Easy-Apply button ──────────────────────
      var externalBtn =
        document.querySelector('.jobs-apply-button a[href]') ||
        document.querySelector('.apply-button a[href]') ||
        (function() {
          var btns = Array.from(document.querySelectorAll('a, button'));
          return btns.find(function(b) {
            return /apply on company site|apply now/i.test(b.textContent) && b.offsetParent !== null;
          });
        })();

      if (externalBtn) {
        var url = externalBtn.href || '';
        AndroidBridge.onResult('easy_apply', JSON.stringify({ action: 'external', url: url }));
        return;
      }

      AndroidBridge.onError('easy_apply', 'No apply button found on page: ' + window.location.href);
      return;
    }

    // ── 4. Modal is open — inspect the current page ───────────────────────────
    // Collect all form fields with labels
    var fields = [];
    var inputs = modal.querySelectorAll(
      'input:not([type=hidden]):not([type=submit]), select, textarea'
    );
    inputs.forEach(function(input) {
      fields.push({
        type: input.tagName.toLowerCase(),
        inputType: input.type || '',
        name: input.name || input.id || '',
        label: findLabel(input),
        value: input.value || '',
        id: input.id || ''
      });
    });

    // Detect submit vs next button
    var submitBtn =
      modal.querySelector('button[aria-label*="Submit application"]') ||
      modal.querySelector('button[aria-label*="Submit"]') ||
      (function() {
        var btns = Array.from(modal.querySelectorAll('button'));
        return btns.find(function(b) {
          return /submit application|submit/i.test(b.getAttribute('aria-label') || b.textContent) &&
                 b.offsetParent !== null && !b.disabled;
        });
      })();

    var nextBtn =
      modal.querySelector('button[aria-label*="Continue to next step"]') ||
      modal.querySelector('button[aria-label*="Next"]') ||
      modal.querySelector('button[aria-label*="Review"]') ||
      modal.querySelector('button[aria-label*="Continue"]') ||
      (function() {
        var btns = Array.from(modal.querySelectorAll('button'));
        return btns.find(function(b) {
          return /continue|next|review/i.test(b.getAttribute('aria-label') || b.textContent) &&
                 b.offsetParent !== null && !b.disabled;
        });
      })();

    AndroidBridge.onResult('easy_apply', JSON.stringify({
      action: 'form_page',
      fields: fields,
      hasSubmit: !!submitBtn,
      hasNext: !!nextBtn,
      pageUrl: window.location.href
    }));

  } catch(e) {
    AndroidBridge.onError('easy_apply', 'Exception: ' + e.message + ' at ' + window.location.href);
  }

  function findLabel(el) {
    if (el.id) {
      var lbl = document.querySelector('label[for="' + el.id + '"]');
      if (lbl) return lbl.textContent.trim();
    }
    var parent = el.closest(
      '.fb-form-element, .artdeco-text-input--container, ' +
      '.jobs-easy-apply-form-element, label, [class*="form-element"]'
    );
    if (parent) return parent.textContent.trim().substring(0, 120);
    return '';
  }
})();
