(function() {
  try {
    var jobs = [];
    var cards = document.querySelectorAll('article.job_result, .job-listing, [data-job-id]');

    cards.forEach(function(card) {
      var jobId = card.getAttribute('data-job-id') || card.id || '';
      var titleEl = card.querySelector('h2 a, .job_title a, a[class*="job"]');
      var companyEl = card.querySelector('.hiring_company_text, .company, a[class*="company"]');

      if (titleEl) {
        jobs.push({
          id: jobId || ('zr_' + jobs.length),
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
