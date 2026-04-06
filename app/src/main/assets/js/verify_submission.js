(function() {
  try {
    var body  = (document.body ? document.body.innerText : '').toLowerCase();
    var title = document.title.toLowerCase();
    var url   = window.location.href.toLowerCase();

    // LinkedIn Easy Apply success strings (updated 2024/2025)
    var successTexts = [
      'your application was sent',
      'application was sent',
      "done! your application",
      "you've applied",
      'you have applied',
      // Generic ATS
      'thank you for applying',
      'thank you for your application',
      'thanks for applying',
      'application submitted',
      'application received',
      'successfully applied',
      'application complete',
      'application confirmed',
      'application sent',
      'submitted successfully',
      'your application has been',
      "we'll review",
      'we will review',
      'application is complete',
      'we received your',
      'application was submitted'
    ];

    var foundText = successTexts.find(function(p) {
      return body.indexOf(p) !== -1 || title.indexOf(p) !== -1;
    });
    var foundUrl = /success|thank|confirm|submitted|complete|applied/.test(url);

    AndroidBridge.onResult('verify_submit', JSON.stringify({
      verified: !!(foundText || foundUrl),
      indicator: foundText || (foundUrl ? 'url_pattern' : 'none'),
      pageTitle: document.title,
      pageUrl: window.location.href,
      bodySnippet: (document.body ? document.body.innerText : '').substring(0, 400)
    }));
  } catch(e) {
    AndroidBridge.onError('verify_submit', e.message);
  }
})();
