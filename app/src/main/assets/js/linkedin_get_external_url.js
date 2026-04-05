(function() {
  try {
    // Look for the external apply button and extract its URL
    var applyUrl = null;

    // Check for "Apply" button that opens external URL (not Easy Apply)
    var applyBtns = document.querySelectorAll(
      '.jobs-apply-button, [data-control-name="jobdetails_topcard_inapply"], ' +
      'a[href*="/jobs/view/externalApply"], button.jobs-apply-button--top-card'
    );

    for (var i = 0; i < applyBtns.length; i++) {
      var btn = applyBtns[i];
      // Skip Easy Apply buttons
      if (btn.textContent.toLowerCase().includes('easy apply')) continue;
      var href = btn.getAttribute('href') || btn.getAttribute('data-job-apply-url');
      if (href && href.startsWith('http')) {
        applyUrl = href;
        break;
      }
    }

    // Try the apply link directly embedded in job detail
    if (!applyUrl) {
      var externalLink = document.querySelector(
        '[data-tracking-control-name="public_jobs_apply-link-offsite_sign-up-modal"],' +
        'a[href*="externalApply"]'
      );
      if (externalLink) applyUrl = externalLink.href;
    }

    // Try to find via LinkedIn's job detail API URL pattern
    if (!applyUrl) {
      var jobId = window.location.pathname.match(/\/jobs\/view\/(\d+)/)?.[1];
      if (jobId) {
        // Check if there's a redirect URL baked into the page data
        var scripts = document.querySelectorAll('script[type="application/ld+json"]');
        for (var s = 0; s < scripts.length; s++) {
          try {
            var data = JSON.parse(scripts[s].textContent);
            if (data.url && !data.url.includes('linkedin.com')) {
              applyUrl = data.url;
              break;
            }
            if (data.applicationContact && data.applicationContact.url) {
              applyUrl = data.applicationContact.url;
              break;
            }
          } catch(e) {}
        }
      }
    }

    if (applyUrl) {
      AndroidBridge.onResult('get_ext_url', applyUrl);
    } else {
      AndroidBridge.onResult('get_ext_url', '');
    }
  } catch(e) {
    AndroidBridge.onError('get_ext_url', e.message);
  }
})();
