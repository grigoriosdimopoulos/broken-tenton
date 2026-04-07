(function() {
  try {
    // Helper: get visible, deduplicated text from an element
    function visibleText(el) {
      if (!el) return '';
      var t = (el.innerText || '').trim();
      if (t) return t.substring(0, 80);
      var c = el.cloneNode(true);
      c.querySelectorAll('[aria-hidden="true"],.visually-hidden,.sr-only').forEach(function(n){n.remove();});
      return c.textContent.trim().substring(0, 80);
    }

    // Helper: unique CSS selector for an element
    function selector(el) {
      if (el.id) return '#' + el.id;
      if (el.name) return el.tagName.toLowerCase() + '[name="' + el.name + '"]';
      // walk up tree to build a path
      var parts = [];
      var cur = el;
      for (var i = 0; i < 4 && cur && cur !== document.body; i++) {
        var tag = cur.tagName.toLowerCase();
        if (cur.id) { parts.unshift('#' + cur.id); break; }
        var cls = Array.from(cur.classList).filter(function(c){
          return c.length > 2 && !/^(artdeco|ember|linkedin|fb-)/.test(c);
        }).slice(0, 2).join('.');
        parts.unshift(cls ? tag + '.' + cls : tag);
        cur = cur.parentElement;
      }
      return parts.join(' > ');
    }

    var buttons = [];
    document.querySelectorAll('button:not([disabled]), input[type=submit]:not([disabled]), input[type=button]:not([disabled]), a[role=button]').forEach(function(el, i) {
      if (el.offsetParent === null) return; // not visible
      buttons.push({
        i: buttons.length,
        text: visibleText(el).substring(0, 60),
        sel: selector(el),
        type: el.type || el.getAttribute('role') || ''
      });
      if (buttons.length >= 15) return;
    });

    var inputs = [];
    document.querySelectorAll('input, textarea, select').forEach(function(el) {
      if (el.offsetParent === null) return;
      if (el.type === 'hidden') return;
      var labelEl = el.id ? document.querySelector('label[for="' + el.id + '"]') : null;
      var ariaLabel = el.getAttribute('aria-label') || el.getAttribute('aria-labelledby') || '';
      if (ariaLabel && !labelEl) {
        var ref = document.getElementById(ariaLabel);
        if (ref) ariaLabel = visibleText(ref);
      }
      inputs.push({
        sel: selector(el),
        type: (el.tagName === 'SELECT' ? 'select' : (el.type || el.tagName.toLowerCase())),
        label: labelEl ? visibleText(labelEl) : ariaLabel,
        placeholder: el.placeholder || '',
        required: !!el.required,
        value: el.value ? el.value.substring(0, 30) : '',
        options: el.tagName === 'SELECT'
          ? Array.from(el.options).slice(0,10).map(function(o){return {val:o.value,text:o.text.trim()};})
          : []
      });
      if (inputs.length >= 20) return;
    });

    var hasModal = !!(
      document.querySelector('[role=dialog]') ||
      document.querySelector('.artdeco-modal') ||
      document.querySelector('[class*="easy-apply-modal"]') ||
      document.querySelector('[class*="easyApply"]')
    );

    var headings = Array.from(document.querySelectorAll('h1,h2,h3,h4'))
      .filter(function(h){ return h.offsetParent !== null; })
      .map(function(h){ return visibleText(h); })
      .filter(function(t){ return t.length > 1; })
      .slice(0, 6);

    var alerts = Array.from(document.querySelectorAll('[role=alert],[role=status],.error,.form-error'))
      .filter(function(el){ return el.offsetParent !== null; })
      .map(function(el){ return visibleText(el).substring(0, 100); })
      .filter(function(t){ return t.length > 0; })
      .slice(0, 5);

    var ctx = {
      url:      location.href,
      title:    document.title.substring(0, 120),
      headings: headings,
      alerts:   alerts,
      bodyText: (document.body.innerText || '').replace(/\s+/g,' ').substring(0, 600),
      hasModal: hasModal,
      buttons:  buttons,
      inputs:   inputs
    };

    AndroidBridge.onResult('extract_ctx', JSON.stringify(ctx));
  } catch(e) {
    AndroidBridge.onError('extract_ctx', e.message || String(e));
  }
})();
