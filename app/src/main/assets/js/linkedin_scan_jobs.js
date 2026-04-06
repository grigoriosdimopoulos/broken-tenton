(function() {
  try {
    var jobs = [];
    var jobCards = document.querySelectorAll(
      '.jobs-search-results__list-item, .job-card-container, [data-job-id], .scaffold-layout__list-item'
    );

    jobCards.forEach(function(card) {
      var jobId = card.getAttribute('data-job-id') ||
                  card.querySelector('[data-job-id]')?.getAttribute('data-job-id') || '';
      var titleEl  = card.querySelector('.job-card-list__title, .job-card-container__link, h3 a, h3');
      var companyEl = card.querySelector('.job-card-container__primary-description, .job-card-list__company-name, h4');
      var easyApplyBadge = card.querySelector('.job-card-container__apply-method, .jobs-apply-button, [aria-label*="Easy Apply"]');
      var linkEl   = card.querySelector('a[href*="/jobs/view/"]');

      // Extract location text (city/country shown below company name)
      var locationEl = card.querySelector(
        '.job-card-container__metadata-item, ' +
        '.artdeco-entity-lockup__caption, ' +
        '.job-card-list__footer-wrapper .job-card-container__metadata-item, ' +
        '[class*="metadata"] li:first-child, ' +
        '.job-card-container__metadata-wrapper li'
      );
      var jobLocation = locationEl ? locationEl.textContent.trim() : '';

      if (!jobId && linkEl) {
        var match = linkEl.href.match(/\/jobs\/view\/(\d+)/);
        if (match) jobId = match[1];
      }

      if (jobId) {
        jobs.push({
          id: jobId,
          title: titleEl   ? titleEl.textContent.trim()   : 'Unknown',
          company: companyEl ? companyEl.textContent.trim() : 'Unknown',
          location: jobLocation,
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
