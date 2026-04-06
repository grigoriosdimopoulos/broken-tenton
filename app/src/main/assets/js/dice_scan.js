(function() {
  try {
    var jobs = [];
    var cards = document.querySelectorAll(
      'dhi-search-result, .card, [data-testid="searchResult"], .search-result-item'
    );

    cards.forEach(function(card) {
      var titleEl = card.querySelector(
        '[data-testid="title-link"], a.card-title-link, h5 a, a[id*="job-title"]'
      );
      var companyEl = card.querySelector(
        '[data-testid="text-company-name"], a.employer-name, .employer-name'
      );
      var locationEl = card.querySelector(
        '[data-testid="text-location"], .location, .search-result-location'
      );

      if (titleEl) {
        var href = titleEl.href || '';
        var jobId = (href.match(/\/([a-f0-9\-]{20,})/i) || [])[1] || ('dice_' + jobs.length);
        jobs.push({
          id: jobId,
          title: titleEl.textContent.trim(),
          company: companyEl ? companyEl.textContent.trim() : 'Unknown',
          isEasyApply: false,
          url: href || window.location.href,
          location: locationEl ? locationEl.textContent.trim() : ''
        });
      }
    });

    AndroidBridge.onResult('scan_jobs', JSON.stringify(jobs));
  } catch(e) {
    AndroidBridge.onError('scan_jobs', e.message);
  }
})();
