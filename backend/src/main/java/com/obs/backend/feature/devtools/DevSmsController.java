package com.obs.backend.feature.devtools;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only "phone screen" for demos. Open http://localhost:8080/dev/sms on a second screen while
 * signing in; new codes appear within two seconds. Not registered outside the dev profile.
 */
@RestController
@Profile("dev")
public class DevSmsController {

    private static final String PAGE =
            """
            <!doctype html>
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>OBS dev SMS inbox</title>
            <style>
              body { margin: 0; font-family: system-ui, sans-serif; background: #0f172a; color: #e2e8f0; display: flex; justify-content: center; }
              main { width: 100%; max-width: 420px; padding: 24px 16px; }
              h1 { font-size: 18px; margin: 0 0 4px; }
              p.hint { margin: 0 0 20px; color: #94a3b8; font-size: 13px; }
              .msg { background: #1e293b; border-radius: 16px; padding: 14px 16px; margin-bottom: 12px; }
              .msg.latest { border: 1px solid #34d399; }
              .meta { display: flex; justify-content: space-between; font-size: 12px; color: #94a3b8; margin-bottom: 6px; }
              .code { font-size: 34px; font-weight: 700; letter-spacing: 6px; color: #34d399; margin: 4px 0; }
              .text { font-size: 14px; }
              .empty { color: #64748b; text-align: center; margin-top: 48px; }
            </style>
            </head>
            <body>
            <main>
              <h1>SMS inbox (dev)</h1>
              <p class="hint">Every OTP the backend would text appears here. Refreshes every 2 seconds.</p>
              <div id="list"><p class="empty">No messages yet.</p></div>
            </main>
            <script>
              const list = document.getElementById('list');
              function el(tag, cls, text) {
                const e = document.createElement(tag);
                if (cls) e.className = cls;
                if (text !== undefined) e.textContent = text;
                return e;
              }
              async function refresh() {
                try {
                  const res = await fetch('/dev/sms.json');
                  const items = await res.json();
                  list.replaceChildren();
                  if (items.length === 0) {
                    list.append(el('p', 'empty', 'No messages yet.'));
                    return;
                  }
                  items.forEach((m, i) => {
                    const card = el('div', i === 0 ? 'msg latest' : 'msg');
                    const meta = el('div', 'meta');
                    meta.append(el('span', '', 'To ' + m.phone), el('span', '', new Date(m.sentAt).toLocaleTimeString()));
                    card.append(meta);
                    const found = /\\d{6}/.exec(m.message);
                    if (found) card.append(el('div', 'code', found[0]));
                    card.append(el('div', 'text', m.message));
                    list.append(card);
                  });
                } catch (e) { /* backend restarting; try again on the next tick */ }
              }
              refresh();
              setInterval(refresh, 2000);
            </script>
            </body>
            </html>
            """;

    private final DevSmsOutbox outbox;

    public DevSmsController(DevSmsOutbox outbox) {
        this.outbox = outbox;
    }

    @GetMapping(value = "/dev/sms", produces = MediaType.TEXT_HTML_VALUE)
    public String page() {
        return PAGE;
    }

    @GetMapping(value = "/dev/sms.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DevSmsOutbox.DevSms> messages() {
        return outbox.recent();
    }
}
