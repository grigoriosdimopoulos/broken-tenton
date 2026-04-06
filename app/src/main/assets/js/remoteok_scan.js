(function() {
  try {
    var jobs = [];
    var rows = document.querySelectorAll('tr.job[data-slug]');

    rows.forEach(function(row) {
      var slug = row.getAttribute('data-slug') || '';
      var titleEl = row.querySelector('h2[itemprop="title"], .company_and_position h2');
      var companyEl = row.querySelector('h3[itemprop="name"], .company h3');
      var tagsEl = row.querySelectorAll('.tags .tag');
      var tags = Array.from(tagsEl).map(function(t){ return t.textContent.trim(); }).join(', ');

      if (titleEl) {
        jobs.push({
          id: slug || ('rok_' + jobs.length),
          title: titleEl.textContent.trim(),
          company: companyEl ? companyEl.textContent.trim() : 'Unknown',
          isEasyApply: false,
          url: 'https://remoteok.com/jobs/' + slug,
          location: 'Remote'
        });
      }
    });

    AndroidBridge.onResult('scan_jobs', JSON.stringify(jobs));
  } catch(e) {
    AndroidBridge.onError('scan_jobs', e.message);
  }
})();
