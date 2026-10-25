package nl.cita.magister.auth

/**
 * Injected into every JavaFX WebView document. Microsoft/IdentityServer
 * form_post pages auto-submit with `HTMLFormElement.submit()`, which does
 * **not** fire the `submit` event — both hooks are required.
 */
object M6loappFormHook {
    val INJECT_JS = """
        (function() {
          function formAction(form) {
            try {
              return (form.getAttribute('action') || form.action || '').toString();
            } catch (e) { return ''; }
          }
          function isM6(action) {
            if (!action) return false;
            var a = action.toLowerCase();
            return a.indexOf('m6loapp:') === 0;
          }
          function serialize(form) {
            var parts = [];
            var els = form.elements;
            for (var i = 0; i < els.length; i++) {
              var el = els[i];
              if (!el || !el.name || el.disabled) continue;
              var type = (el.type || '').toLowerCase();
              if (type === 'submit' || type === 'button' || type === 'reset' || type === 'image') continue;
              if ((type === 'checkbox' || type === 'radio') && !el.checked) continue;
              parts.push(encodeURIComponent(el.name) + '=' + encodeURIComponent(el.value || ''));
            }
            return parts.join('&');
          }
          function send(form) {
            try {
              var action = formAction(form);
              if (!isM6(action)) return false;
              var body = serialize(form);
              if (window.magsBridge && window.magsBridge.onFormPost) {
                window.magsBridge.onFormPost(action, body);
              }
              return true;
            } catch (e) { return false; }
          }
          var proto = window.HTMLFormElement && HTMLFormElement.prototype;
          if (proto && !proto.__magsSubmitPatched) {
            var original = proto.submit;
            proto.submit = function() {
              if (send(this)) return;
              return original.apply(this, arguments);
            };
            proto.__magsSubmitPatched = true;
          }
          if (!window.__magsSubmitListener) {
            window.__magsSubmitListener = true;
            document.addEventListener('submit', function(e) {
              if (e.target && send(e.target)) {
                e.preventDefault();
                e.stopImmediatePropagation();
                e.stopPropagation();
              }
            }, true);
          }
          var forms = document.getElementsByTagName('form');
          for (var i = 0; i < forms.length; i++) {
            if (send(forms[i])) break;
          }
          return window.__magsHooked ? 'already' : (window.__magsHooked = true, 'hooked');
        })();
    """.trimIndent()

    val CAPTURE_JS = """
        (function() {
          var forms = document.getElementsByTagName('form');
          for (var i = 0; i < forms.length; i++) {
            var a = (forms[i].getAttribute('action') || forms[i].action || '').toString();
            if (a.toLowerCase().indexOf('m6loapp:') !== 0) continue;
            var parts = [];
            var els = forms[i].elements;
            for (var j = 0; j < els.length; j++) {
              var el = els[j];
              if (!el || !el.name) continue;
              parts.push(encodeURIComponent(el.name) + '=' + encodeURIComponent(el.value || ''));
            }
            return a + '\n' + parts.join('&');
          }
          return null;
        })();
    """.trimIndent()

    fun parseCaptureResult(result: Any?): Pair<String, String>? {
        val text = result?.toString().orEmpty()
        if (text.isBlank() || text == "null" || text == "undefined") return null
        val nl = text.indexOf('\n')
        val action = if (nl >= 0) text.substring(0, nl) else text
        val body = if (nl >= 0) text.substring(nl + 1) else ""
        return action to body
    }
}
