(function() {
  try {
    var jobs = [];
    var cards = document.querySelectorAll('[data-test="jobListing"], .react-job-listing, li[data-id]');

    cards.forEach(function(card) {
      var jobId = card.getAttribute('data-id') || card.getAttribute('data-job-id') || '';
      var titleEl = card.querySelector('[data-test="job-title"], .job-title a, a[href*="/job-listing/"]');
      var companyEl = card.querySelector('[data-test="employer-name"], .employer-name');

      if (titleEl) {
        var href = titleEl.href || (titleEl.closest('a') ? titleEl.closest('a').href : '');
        if (!jobId && href) {
          var match = href.match(/jobListingId=(\d+)/);
          if (match) jobId = match[1];
        }
        jobs.push({
          id: jobId || ('gd_' + jobs.length),
          title: titleEl.textContent.trim(),
          company: companyEl ? companyEl.textContent.trim() : 'Unknown',
          isEasyApply: false,
          url: href || window.location.href
        });
      }
    });

    AndroidBridge.onResult('scan_jobs', JSON.stringify(jobs));
  } catch(e) {
    AndroidBridge.onError('scan_jobs', e.message);
  }
})();
