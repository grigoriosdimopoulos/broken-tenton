(function() {
  try {
    var jobs = [];
    var listings = document.querySelectorAll('ul#job_list li.feature, ul.jobs li:not(.view-all)');

    listings.forEach(function(li) {
      var titleEl = li.querySelector('span.title, .title');
      var companyEl = li.querySelector('span.company, .company');
      var linkEl = li.querySelector('a[href*="/jobs/"]');

      if (titleEl && linkEl) {
        var href = linkEl.href || '';
        var jobId = (href.match(/\/jobs\/([^/]+)/) || [])[1] || ('wwr_' + jobs.length);
        jobs.push({
          id: jobId,
          title: titleEl.textContent.trim(),
          company: companyEl ? companyEl.textContent.trim() : 'Unknown',
          isEasyApply: false,
          url: href,
          location: 'Remote / Worldwide'
        });
      }
    });

    AndroidBridge.onResult('scan_jobs', JSON.stringify(jobs));
  } catch(e) {
    AndroidBridge.onError('scan_jobs', e.message);
  }
})();
