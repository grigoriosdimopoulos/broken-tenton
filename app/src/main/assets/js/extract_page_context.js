(function() {
  // Always call onResult (never onError) so runJs never times out.
  // Even on exception, return what we know.
  try {
    // Simple selector builder
    function sel(el) {
      if (el.id) return '#' + el.id;
      if (el.name) return el.tagName.toLowerCase() + '[name="' + el.name.replace(/"/g,'\\"') + '"]';
      var cls = (el.className || '').split(' ').filter(function(c){ return c.length > 2; })[0] || '';
      return cls ? el.tagName.toLowerCase() + '.' + cls : el.tagName.toLowerCase();
    }

    var buttons = [];
    var btnEls = document.querySelectorAll('button, input[type=submit], input[type=button], [role=button], a[role=button]');
    for (var i = 0; i < btnEls.length && buttons.length < 12; i++) {
      var b = btnEls[i];
      if (b.disabled) continue;
      var txt = (b.innerText || b.value || b.getAttribute('aria-label') || '').trim().replace(/\s+/g,' ');
      if (!txt) continue;
      buttons.push({ i: buttons.length, text: txt.substring(0, 60), sel: sel(b) });
    }

    var inputs = [];
    var inpEls = document.querySelectorAll('input, textarea, select');
    for (var j = 0; j < inpEls.length && inputs.length < 12; j++) {
      var inp = inpEls[j];
      if (inp.type === 'hidden') continue;
      var lbl = '';
      if (inp.id) {
        var labelEl = document.querySelector('label[for="' + inp.id + '"]');
        if (labelEl) lbl = (labelEl.innerText || '').trim();
      }
      if (!lbl) lbl = inp.getAttribute('aria-label') || inp.placeholder || inp.name || '';
      var opts = [];
      if (inp.tagName === 'SELECT') {
        for (var k = 0; k < Math.min(inp.options.length, 6); k++) {
          opts.push({ val: inp.options[k].value, txt: inp.options[k].text.trim() });
        }
      }
      inputs.push({
        sel: sel(inp),
        type: inp.tagName === 'SELECT' ? 'select' : (inp.type || inp.tagName.toLowerCase()),
        label: lbl.substring(0, 60),
        required: !!inp.required,
        value: (inp.value || '').substring(0, 40),
        options: opts
      });
    }

    var headings = [];
    var hEls = document.querySelectorAll('h1, h2, h3');
    for (var h = 0; h < hEls.length && headings.length < 6; h++) {
      var ht = (hEls[h].innerText || '').trim();
      if (ht) headings.push(ht.substring(0, 80));
    }

    var hasModal = !!(
      document.querySelector('[role=dialog]') ||
      document.querySelector('.artdeco-modal') ||
      document.querySelector('[class*="easy-apply"]') ||
      document.querySelector('[class*="modal"][style*="display: block"]')
    );

    var ctx = {
      url:      location.href.substring(0, 300),
      title:    document.title.substring(0, 100),
      headings: headings,
      hasModal: hasModal,
      buttons:  buttons,
      inputs:   inputs,
      body:     (document.body ? document.body.innerText : '').replace(/\s+/g,' ').substring(0, 400)
    };

    AndroidBridge.onResult('extract_ctx', JSON.stringify(ctx));
  } catch(e) {
    // Still return result so runJs doesn't time out
    AndroidBridge.onResult('extract_ctx', JSON.stringify({
      error: String(e),
      url: location.href || '',
      buttons: [], inputs: [], headings: [], hasModal: false, body: ''
    }));
  }
})();
