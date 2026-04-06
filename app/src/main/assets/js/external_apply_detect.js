(function() {
  try {
    var fields = [];

    var inputs = document.querySelectorAll(
      'input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=image]),' +
      'select, textarea'
    );

    inputs.forEach(function(input, idx) {
      var label = findLabel(input);
      fields.push({
        idx: idx,
        tag: input.tagName.toLowerCase(),
        type: input.type || 'text',
        name: input.name || '',
        id: input.id || '',
        placeholder: input.placeholder || '',
        label: label,
        value: input.value || '',
        required: input.required || false,
        options: input.tagName === 'SELECT'
          ? Array.from(input.options).filter(o => o.value).map(o => ({value: o.value, text: o.textContent.trim()}))
          : undefined
      });
    });

    // --- Submit button detection (ATS-aware) ---
    var submitBtns = [];

    // Workday
    var wdNext = document.querySelector(
      '[data-automation-id="bottom-navigation-next-button"],' +
      '[data-automation-id="bottom-navigation-send-it-button"]'
    );
    if (wdNext) submitBtns.push({id: wdNext.id || '', text: wdNext.textContent.trim(), ats: 'workday'});

    // Greenhouse / iCIMS / Lever / BambooHR
    var atsBtns = document.querySelectorAll(
      '#submit_app, #app-submit-btn, .btn-submit, ' +
      '[data-qa="btn-submit"], [data-testid="submit-app-button"], ' +
      'button[class*="submit"], button[class*="apply"]'
    );
    atsBtns.forEach(function(b) {
      if (!submitBtns.find(x => x.id === b.id)) {
        submitBtns.push({id: b.id || '', text: b.textContent.trim()});
      }
    });

    // Generic type=submit / input[type=submit]
    document.querySelectorAll('button[type=submit], input[type=submit]').forEach(function(b) {
      if (!submitBtns.find(x => x.id === b.id))
        submitBtns.push({id: b.id || '', text: (b.textContent || b.value || '').trim()});
    });

    // Any visible button whose text suggests submission
    if (submitBtns.length === 0) {
      document.querySelectorAll('button').forEach(function(b) {
        var t = (b.textContent || '').trim();
        if (/^(submit|apply|send application|send it|next|continue)$/i.test(t) &&
            b.offsetParent !== null && !b.disabled) {
          submitBtns.push({id: b.id || '', text: t});
        }
      });
    }

    AndroidBridge.onResult('external_detect', JSON.stringify({
      fields: fields,
      submitButtons: submitBtns,
      pageUrl: window.location.href
    }));

  } catch(e) {
    AndroidBridge.onError('external_detect', e.message);
  }

  function findLabel(el) {
    if (el.id) {
      var lbl = document.querySelector('label[for="' + el.id + '"]');
      if (lbl) return lbl.textContent.trim().replace(/[*\n\t]+/g, ' ').trim();
    }
    var prev = el.previousElementSibling;
    if (prev && (prev.tagName === 'LABEL' || prev.tagName === 'SPAN')) return prev.textContent.trim();
    var parent = el.closest('[class*="form"], [class*="field"], [class*="input"], fieldset, [class*="Field"], [class*="Form"]');
    if (parent) {
      var lbl = parent.querySelector('label, legend, [class*="label"], [class*="Label"]');
      if (lbl && !lbl.contains(el)) return lbl.textContent.trim().replace(/[*\n\t]+/g, ' ').trim();
    }
    return el.placeholder || el.name || '';
  }
})();
