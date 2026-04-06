(function() {
  try {
    var jobs = [];
    var cards = document.querySelectorAll('.job_seen_beacon, .result, [data-jk], .jobsearch-SerpJobCard');

    cards.forEach(function(card) {
      var jobId = card.getAttribute('data-jk') || card.id || '';
      var titleEl = card.querySelector('h2 a span, .jobTitle a, a[data-jk]');
      var companyEl = card.querySelector('.companyName, [data-testid="company-name"]');
      var linkEl = card.querySelector('h2 a, .jobTitle a, a[href*="/jobs/"]');
      var applyEl = card.querySelector('[aria-label*="Easily apply"], .ia-IndeedApplyButton, [data-indeed-apply]');

      var locationEl = card.querySelector('.companyLocation, [data-testid="text-location"], .resultContent .attribute_snippet');
      if (titleEl || linkEl) {
        var href = linkEl ? linkEl.href : '';
        if (!jobId && href) {
          var match = href.match(/[?&]jk=([a-z0-9]+)/i);
          if (match) jobId = match[1];
        }
        jobs.push({
          id: jobId || ('indeed_' + jobs.length),
          title: titleEl ? titleEl.textContent.trim() : (linkEl ? linkEl.textContent.trim() : 'Unknown'),
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
