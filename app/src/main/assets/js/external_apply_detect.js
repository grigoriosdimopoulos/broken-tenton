(function() {
  try {
    var fields = [];

    var inputs = document.querySelectorAll(
      'input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=image]), select, textarea'
    );

    inputs.forEach(function(input, idx) {
      var label = findLabel(input);
      var fieldInfo = {
        idx: idx,
        tag: input.tagName.toLowerCase(),
        type: input.type || 'text',
        name: input.name || '',
        id: input.id || '',
        placeholder: input.placeholder || '',
        label: label,
        value: input.value || '',
        required: input.required || false
      };

      if (input.tagName === 'SELECT') {
        var options = [];
        input.querySelectorAll('option').forEach(function(opt) {
          if (opt.value) options.push({value: opt.value, text: opt.textContent.trim()});
        });
        fieldInfo.options = options;
      }

      fields.push(fieldInfo);
    });

    var submitBtns = [];
    document.querySelectorAll('button[type=submit], input[type=submit], button').forEach(function(btn) {
      var text = btn.textContent.trim() || btn.value || '';
      if (/submit|apply|send|continue|next/i.test(text)) {
        submitBtns.push({id: btn.id, text: text, idx: Array.from(document.querySelectorAll('button')).indexOf(btn)});
      }
    });

    AndroidBridge.onResult('external_detect', JSON.stringify({fields: fields, submitButtons: submitBtns}));
  } catch(e) {
    AndroidBridge.onError('external_detect', e.message);
  }

  function findLabel(el) {
    if (el.id) {
      var lbl = document.querySelector('label[for="' + el.id + '"]');
      if (lbl) return lbl.textContent.trim().replace(/[*\n\t]+/g, ' ').trim();
    }
    var prev = el.previousElementSibling;
    if (prev && prev.tagName === 'LABEL') return prev.textContent.trim();
    var parent = el.closest('[class*="form"], [class*="field"], [class*="input"], fieldset');
    if (parent) {
      var lbl = parent.querySelector('label, legend, [class*="label"]');
      if (lbl && !lbl.contains(el)) return lbl.textContent.trim().replace(/[*\n\t]+/g, ' ').trim();
    }
    return el.placeholder || el.name || '';
  }
})();
