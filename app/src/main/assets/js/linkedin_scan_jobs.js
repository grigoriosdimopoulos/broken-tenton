(function() {
  try {
    // Helper: return the clean, visible text from an element, avoiding
    // hidden accessibility duplicates (aria-hidden spans, visually-hidden spans)
    function cleanText(el) {
      if (!el) return '';
      // Prefer innerText: skips CSS-hidden content (visually-hidden, sr-only, etc.)
      var t = (el.innerText || '').trim();
      if (t) return t;
      // Fallback: remove known hidden child spans before using textContent
      var clone = el.cloneNode(true);
      clone.querySelectorAll('[aria-hidden="true"], .visually-hidden, .sr-only, [class*="hidden"]')
           .forEach(function(n) { n.remove(); });
      return clone.textContent.trim();
    }

    var jobs = [];
    var jobCards = document.querySelectorAll(
      '.jobs-search-results__list-item, .job-card-container, [data-job-id], .scaffold-layout__list-item'
    );

    jobCards.forEach(function(card) {
      var jobId = card.getAttribute('data-job-id') || '';
      if (!jobId) {
        var dataJobEl = card.querySelector('[data-job-id]');
        if (dataJobEl) jobId = dataJobEl.getAttribute('data-job-id') || '';
      }

      // Title: try most specific selector first, then fall back
      var titleEl = card.querySelector(
        '.job-card-list__title--link strong, ' +
        '.job-card-list__title--link, ' +
        '.job-card-list__title, ' +
        '.job-card-container__link strong, ' +
        '.job-card-container__link, ' +
        'h3 a strong, h3 a, h3'
      );

      // Company: LinkedIn 2025 DOM — try multiple selectors
      var companyEl = card.querySelector(
        '.artdeco-entity-lockup__subtitle span, ' +
        '.artdeco-entity-lockup__subtitle, ' +
        '.job-card-container__primary-description, ' +
        '.job-card-list__company-name, ' +
        '.job-card-container__company-name, ' +
        '.base-search-card__subtitle, ' +
        'h4 a, h4'
      );

      var easyApplyBadge = card.querySelector(
        '.job-card-container__apply-method, ' +
        '.jobs-apply-button, ' +
        '[aria-label*="Easy Apply"], ' +
        '.job-card-container__footer-item--apply'
      );

      var linkEl = card.querySelector('a[href*="/jobs/view/"]');

      // Location: first metadata item under the company
      var locationEl = card.querySelector(
        '.job-card-container__metadata-item, ' +
        '.artdeco-entity-lockup__caption, ' +
        '.job-card-list__footer-wrapper .job-card-container__metadata-item, ' +
        '[class*="metadata"] li:first-child, ' +
        '.job-card-container__metadata-wrapper li'
      );
      var jobLocation = cleanText(locationEl);

      if (!jobId && linkEl) {
        var match = linkEl.href.match(/\/jobs\/view\/(\d+)/);
        if (match) jobId = match[1];
      }

      if (jobId) {
        jobs.push({
          id: jobId,
          title:     cleanText(titleEl)   || 'Unknown',
          company:   cleanText(companyEl) || 'Unknown',
          location:  jobLocation,
          isEasyApply: !!easyApplyBadge,
          url: linkEl ? linkEl.href : ('https://www.linkedin.com/jobs/view/' + jobId)
        });
      }
    });

    AndroidBridge.onResult('scan_jobs', JSON.stringify(jobs));
  } catch(e) {
    AndroidBridge.onError('scan_jobs', e.message);
  }
})();
