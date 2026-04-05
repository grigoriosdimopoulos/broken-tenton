(function() {
  try {
    var jobs = [];
    var cards = document.querySelectorAll('[data-jobid], .job-cardstyle__JobCardComponent, .results-card');

    cards.forEach(function(card) {
      var jobId = card.getAttribute('data-jobid') || card.getAttribute('data-job-id') || '';
      var titleEl = card.querySelector('h2 a, .job-cardstyle__JobTitle a, a[data-test="jobTitle"]');
      var companyEl = card.querySelector('.job-cardstyle__CompanyName, [data-test="company"]');

      if (titleEl) {
        jobs.push({
          id: jobId || ('mn_' + jobs.length),
          title: titleEl.textContent.trim(),
          company: companyEl ? companyEl.textContent.trim() : 'Unknown',
          isEasyApply: false,
          url: titleEl.href || window.location.href
        });
      }
    });

    AndroidBridge.onResult('scan_jobs', JSON.stringify(jobs));
  } catch(e) {
    AndroidBridge.onError('scan_jobs', e.message);
  }
})();
