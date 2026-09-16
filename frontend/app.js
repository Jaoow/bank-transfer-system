// ──────────────────────────────────────────
// CONFIG
// ──────────────────────────────────────────
const ACCOUNT_SERVICE_URL  = 'http://localhost:8080';
const TRANSFER_SERVICE_URL = 'http://localhost:8081';
const POLL_INTERVAL_MS     = 2000;

// ──────────────────────────────────────────
// STATE
// ──────────────────────────────────────────
let prevAccounts  = {};   // id → balance
let prevTransfers = {};   // id → status
const topicCounts = {};
const MAX_LOG_ENTRIES = 30;

// ──────────────────────────────────────────
// PARTICLE SYSTEM
// ──────────────────────────────────────────
const canvas = document.getElementById('particle-canvas');
const ctx    = canvas.getContext('2d');
const particles = [];

function resizeCanvas() {
  canvas.width  = canvas.offsetWidth;
  canvas.height = canvas.offsetHeight;
}

window.addEventListener('resize', resizeCanvas);
resizeCanvas();

const TOPIC_COLORS = {
  'transfer-requested':       '#4f8ef7',
  'debit-reserved':           '#4f8ef7',
  'debit-failed':             '#f75f5f',
  'credit-requested':         '#f7a94f',
  'credit-applied':           '#3ecf8e',
  'credit-failed':            '#f75f5f',
  'debit-reversal-requested': '#f7a94f',
  'debit-reversed':           '#f7a94f',
};

function spawnParticle(topic) {
  const color = TOPIC_COLORS[topic] || '#94a3b8';
  const topicRows = document.querySelectorAll('.topic-row');
  let y = canvas.height / 2;
  for (const row of topicRows) {
    if (row.dataset.topic === topic) {
      const rect = row.getBoundingClientRect();
      const canvasRect = canvas.getBoundingClientRect();
      y = (rect.top + rect.height / 2) - canvasRect.top;
      break;
    }
  }

  for (let i = 0; i < 5; i++) {
    particles.push({
      x:     0,
      y:     y + (Math.random() - 0.5) * 20,
      vx:    2.5 + Math.random() * 2.5,
      vy:    (Math.random() - 0.5) * 0.5,
      alpha: 0.9,
      size:  2.5 + Math.random() * 2,
      color,
      delay: i * 60,
      born:  Date.now(),
    });
  }
}

function animateParticles() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  const now = Date.now();
  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i];
    if (now - p.born < p.delay) continue;
    p.x     += p.vx;
    p.y     += p.vy;
    p.alpha -= 0.008;
    if (p.alpha <= 0 || p.x > canvas.width + 10) {
      particles.splice(i, 1);
      continue;
    }
    ctx.save();
    ctx.globalAlpha = p.alpha;
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
    ctx.fillStyle = p.color;
    ctx.shadowColor = p.color;
    ctx.shadowBlur  = 6;
    ctx.fill();
    ctx.restore();
  }
  requestAnimationFrame(animateParticles);
}

animateParticles();

// ──────────────────────────────────────────
// SSE — Live Event Stream
// ──────────────────────────────────────────
function connectSSE() {
  const dot   = document.getElementById('connection-dot');
  const label = document.getElementById('connection-label');

  try {
    const es = new EventSource(`${ACCOUNT_SERVICE_URL}/events/stream`);

    es.addEventListener('connected', () => {
      dot.className   = 'status-dot connected';
      label.textContent = 'Live';
    });

    es.addEventListener('kafka-event', (e) => {
      const data = JSON.parse(e.data);
      onKafkaEvent(data.topic, data.key, data.payload, data.timestamp);
    });

    es.onerror = () => {
      dot.className   = 'status-dot error';
      label.textContent = 'Reconnecting...';
      es.close();
      setTimeout(connectSSE, 3000);
    };
  } catch {
    dot.className   = 'status-dot error';
    label.textContent = 'SSE unavailable — polling only';
  }
}

connectSSE();

// ──────────────────────────────────────────
// KAFKA EVENT HANDLER
// ──────────────────────────────────────────
function onKafkaEvent(topic, key, payload, timestamp) {
  // 1. Increment topic counter
  topicCounts[topic] = (topicCounts[topic] || 0) + 1;
  const cntEl = document.getElementById(`cnt-${topic}`);
  if (cntEl) cntEl.textContent = topicCounts[topic];

  // 2. Pulse topic row
  const topicRow = document.querySelector(`.topic-row[data-topic="${topic}"]`);
  if (topicRow) {
    const dot = topicRow.querySelector('.topic-dot');
    topicRow.classList.add('active');
    dot.classList.add('active');
    setTimeout(() => {
      topicRow.classList.remove('active');
      dot.classList.remove('active');
    }, 700);
  }

  // 3. Spawn particles
  spawnParticle(topic);

  // 4. Log entry
  addEventLogEntry(topic, key, timestamp);
}

function addEventLogEntry(topic, key, timestamp) {
  const container = document.getElementById('event-log-entries');
  const ts = timestamp ? new Date(timestamp).toLocaleTimeString() : new Date().toLocaleTimeString();
  const shortKey = key ? key.substring(0, 8) + '…' : '';
  const entry = document.createElement('div');
  entry.className = 'event-log-entry';
  entry.innerHTML = `<span class="evt-topic">${topic}</span> · key: <span class="evt-key">${shortKey}</span> · ${ts}`;
  container.prepend(entry);

  // Max log size
  while (container.children.length > MAX_LOG_ENTRIES) {
    container.removeChild(container.lastChild);
  }
}

// ──────────────────────────────────────────
// ACCOUNTS PANEL
// ──────────────────────────────────────────
async function fetchAccounts() {
  try {
    const resp = await fetch(`${ACCOUNT_SERVICE_URL}/accounts`);
    if (!resp.ok) return;
    const accounts = await resp.json();
    renderAccounts(accounts);
  } catch { /* service might be down */ }
}

function renderAccounts(accounts) {
  const container = document.getElementById('accounts-list');

  // Remove skeletons on first load
  container.querySelectorAll('.skeleton-card').forEach(s => s.remove());

  // Compute max balance for bar scaling
  const maxBalance = Math.max(...accounts.map(a => parseFloat(a.balance)), 1);

  accounts.forEach(account => {
    const id      = account.id;
    const balance = parseFloat(account.balance);
    const prev    = prevAccounts[id];
    const changed = prev !== undefined && prev !== balance;
    const isUp    = balance > (prev ?? balance);
    prevAccounts[id] = balance;

    let card = document.getElementById(`account-${id}`);
    if (!card) {
      card = document.createElement('div');
      card.className = 'account-card';
      card.id = `account-${id}`;
      card.innerHTML = `
        <div class="account-id">${id}</div>
        <div class="account-balance" id="bal-${id}">R$ 0,00</div>
        <div class="balance-bar-track">
          <div class="balance-bar-fill" id="bar-${id}" style="width:0%"></div>
        </div>
      `;
      container.appendChild(card);
    }

    // Animate balance
    const balEl = document.getElementById(`bal-${id}`);
    const barEl = document.getElementById(`bar-${id}`);

    if (changed) {
      card.classList.remove('flash-green', 'flash-red');
      void card.offsetWidth; // reflow
      card.classList.add(isUp ? 'flash-green' : 'flash-red');
    }

    animateBalance(balEl, prev ?? 0, balance);
    barEl.style.width = `${Math.min((balance / maxBalance) * 100, 100)}%`;
  });
}

function animateBalance(el, from, to) {
  const duration = 800;
  const start    = performance.now();
  function step(now) {
    const progress = Math.min((now - start) / duration, 1);
    const eased    = 1 - Math.pow(1 - progress, 3);
    const current  = from + (to - from) * eased;
    el.textContent = `R$ ${current.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

// ──────────────────────────────────────────
// LEDGER (account events)
// ──────────────────────────────────────────
const renderedLedger = new Set();

async function fetchLedger(accountId) {
  try {
    const resp = await fetch(`${ACCOUNT_SERVICE_URL}/accounts/${accountId}/events`);
    if (!resp.ok) return;
    const events = await resp.json();
    const container = document.getElementById('ledger-list');

    events.forEach(evt => {
      if (renderedLedger.has(evt.id)) return;
      renderedLedger.add(evt.id);

      const item = document.createElement('div');
      item.className = 'ledger-item';
      item.innerHTML = `
        <span class="ledger-type ${evt.eventType}">${evt.eventType}</span>
        <span class="ledger-amount">R$ ${parseFloat(evt.amount).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
      `;
      container.prepend(item);
      while (container.children.length > 20) container.removeChild(container.lastChild);
    });
  } catch { /* ignore */ }
}

// ──────────────────────────────────────────
// TRANSFERS PANEL
// ──────────────────────────────────────────
const STATUS_BADGE = {
  REQUESTED:      'requested',
  DEBIT_RESERVED: 'debit-reserved',
  COMPLETED:      'completed',
  COMPENSATING:   'compensating',
  CANCELLED:      'cancelled',
};

async function fetchTransfers() {
  try {
    const resp = await fetch(`${TRANSFER_SERVICE_URL}/transfers`);
    if (!resp.ok) return;
    const transfers = await resp.json();
    renderTransfers(transfers);
  } catch { /* service down */ }
}

function renderTransfers(transfers) {
  const container = document.getElementById('transfers-list');
  if (transfers.length === 0) return;

  // Remove empty state
  container.querySelectorAll('.empty-state').forEach(e => e.remove());

  transfers.forEach(t => {
    const id     = t.id;
    const status = t.status;
    const prev   = prevTransfers[id];
    prevTransfers[id] = status;

    let card = document.getElementById(`transfer-${id}`);
    if (!card) {
      card = document.createElement('div');
      card.className = 'transfer-card';
      card.id = `transfer-${id}`;
      card.innerHTML = `
        <div class="transfer-id">#${id.substring(0, 8)}…</div>
        <div class="transfer-route">
          <span class="from">${(t.originAccountId ?? '?').substring(0,8)}…</span>
          <span class="arrow-icon">→</span>
          <span class="to">${(t.destinationAccountId ?? '?').substring(0,8)}…</span>
        </div>
        <div class="transfer-footer">
          <span class="transfer-amount">R$ ${parseFloat(t.amount).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
          <span class="badge ${STATUS_BADGE[status] ?? 'requested'}" id="badge-${id}">${status}</span>
        </div>
      `;
      container.prepend(card);
    } else if (prev !== status) {
      // Update badge + card state class
      const badgeEl = document.getElementById(`badge-${id}`);
      if (badgeEl) {
        badgeEl.className = `badge ${STATUS_BADGE[status] ?? 'requested'}`;
        badgeEl.textContent = status;
      }
      card.className = `transfer-card state-${status}`;
    }
  });
}

// ──────────────────────────────────────────
// POLLING LOOP
// ──────────────────────────────────────────
async function poll() {
  await fetchAccounts();
  await fetchTransfers();

  // Fetch ledger for all known accounts
  for (const id of Object.keys(prevAccounts)) {
    await fetchLedger(id);
  }
}

poll();
setInterval(poll, POLL_INTERVAL_MS);

// ──────────────────────────────────────────
// MODAL
// ──────────────────────────────────────────
function openModal() {
  document.getElementById('modal-overlay').classList.remove('hidden');
  document.getElementById('modal-feedback').classList.add('hidden');
}

function closeModal(e) {
  if (!e || e.target.id === 'modal-overlay') {
    document.getElementById('modal-overlay').classList.add('hidden');
  }
}

async function submitTransfer() {
  const origin = document.getElementById('input-origin').value.trim();
  const dest   = document.getElementById('input-dest').value.trim();
  const amount = document.getElementById('input-amount').value.trim();
  const feedback = document.getElementById('modal-feedback');
  const btn    = document.getElementById('btn-submit');

  if (!origin || !dest || !amount) {
    showFeedback('All fields are required.', 'error');
    return;
  }

  btn.textContent = 'Sending…';
  btn.disabled = true;

  try {
    const resp = await fetch(`${TRANSFER_SERVICE_URL}/transfers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        originAccountId:      origin,
        destinationAccountId: dest,
        amount:               parseFloat(amount)
      })
    });

    if (resp.ok) {
      const data = await resp.json();
      showFeedback(`✓ Transfer ${data.transferId?.substring(0, 8)}… initiated!`, 'success');
      setTimeout(closeModal, 2000);
    } else {
      showFeedback(`Error ${resp.status}: ${resp.statusText}`, 'error');
    }
  } catch {
    showFeedback('Cannot reach transfer-service. Is it running on :8081?', 'error');
  } finally {
    btn.textContent = 'Send Transfer';
    btn.disabled = false;
  }
}

function showFeedback(msg, type) {
  const el = document.getElementById('modal-feedback');
  el.textContent = msg;
  el.className = `modal-feedback ${type}`;
}
