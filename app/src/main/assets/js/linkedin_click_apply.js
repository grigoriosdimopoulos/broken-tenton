(function() {
  // Locate the external Apply link or button on a LinkedIn job detail page
  // and trigger navigation to it. Does NOT call AndroidBridge — navigation
  // is detected by the Kotlin engine's onPageFinished callback.

  // 1. Direct <a> to LinkedIn's externalApply redirect endpoint
  var extLink = document.querySelector(
    'a[href*="externalApply"], a[href*="external-apply"], ' +
    '[data-tracking-control-name*="apply"][href*="http"]'
  );
  if (extLink && extLink.href && !extLink.href.includes('easyApply')) {
    window.location.href = extLink.href;
    return;
  }

  // 2. Apply button that is NOT "Easy Apply"
  var allBtns = Array.from(document.querySelectorAll(
    'button, a.jobs-apply-button, .jobs-apply-button--top-card'
  ));
  for (var i = 0; i < allBtns.length; i++) {
    var b = allBtns[i];
    var label = (b.textContent || b.getAttribute('aria-label') || '').trim().toLowerCase();
    if ((label === 'apply' || label === 'apply now' || label === 'apply on company website') &&
        !label.includes('easy') && !b.disabled && b.offsetParent !== null) {
      b.click();
      return;
    }
  }

  // 3. No apply button found — signal failure so Kotlin falls back
  AndroidBridge.onResult('click_apply_noop', 'no_button');
})();
