(function() {
  try {
    var questions = [];
    var modal = document.querySelector('.jobs-easy-apply-modal, .jobs-apply-modal') || document;

    // Find open-ended text areas (screening questions)
    var textareas = modal.querySelectorAll('textarea');
    textareas.forEach(function(ta) {
      var label = findLabel(ta);
      if (label) {
        questions.push({
          elementId: ta.id || ta.name || ('ta_' + questions.length),
          question: label,
          type: 'textarea',
          currentValue: ta.value || ''
        });
      }
    });

    // Find text inputs that look like screening questions
    var inputs = modal.querySelectorAll('input[type="text"]');
    inputs.forEach(function(input) {
      var label = findLabel(input);
      if (label && label.endsWith('?') && !label.toLowerCase().includes('phone') && !label.toLowerCase().includes('email')) {
        questions.push({
          elementId: input.id || input.name || ('in_' + questions.length),
          question: label,
          type: 'text',
          currentValue: input.value || ''
        });
      }
    });

    AndroidBridge.onResult('screening', JSON.stringify(questions));
  } catch(e) {
    AndroidBridge.onError('screening', e.message);
  }

  function findLabel(el) {
    if (el.id) {
      var lbl = document.querySelector('label[for="' + el.id + '"]');
      if (lbl) return lbl.textContent.trim();
    }
    var parent = el.closest('.fb-form-element, .artdeco-text-input--container');
    if (parent) {
      var legendOrLabel = parent.querySelector('legend, label, span[data-test-form-element-label-title]');
      if (legendOrLabel) return legendOrLabel.textContent.trim();
      return parent.textContent.trim().substring(0, 200);
    }
    return el.placeholder || '';
  }
})();
