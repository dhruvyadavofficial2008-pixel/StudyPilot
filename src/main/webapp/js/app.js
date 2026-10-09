const $ = s => document.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let user = null, view = 'dashboard', deck = [], idx = 0, timerId = null, secs = 25 * 60;

async function api(url, data) {
  const opt = data ? { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }, body: new URLSearchParams(data) } : {};
  const r = await fetch(url, opt);
  const j = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(j.error || 'Something went wrong. Please try again.');
  return j;
}
function toast(msg, bad) {
  const t = $('#toast'); t.textContent = msg; t.className = 'show' + (bad ? ' bad' : '');
  setTimeout(() => t.className = '', 2600);
}
async function act(url, data, ok) {
  try { await api(url, data); if (ok) toast(ok); return true; }
  catch (e) { toast(e.message, true); return false; }
}

/* ---------- auth ---------- */
function authMode(m) {
  $('#authForm').dataset.mode = m;
  $('#regFields').hidden = m !== 'register';
  $('#authBtn').textContent = m === 'login' ? 'Sign in' : 'Create account';
  $('#tabLogin').classList.toggle('on', m === 'login');
  $('#tabReg').classList.toggle('on', m === 'register');
}
$('#tabLogin').onclick = () => authMode('login');
$('#tabReg').onclick = () => authMode('register');
$('#authForm').onsubmit = async e => {
  e.preventDefault();
  try { user = await api('api/auth/' + e.target.dataset.mode, Object.fromEntries(new FormData(e.target))); enter(); }
  catch (x) { toast(x.message, true); }
};
$('#logout').onclick = async () => { await api('api/auth/logout', {}); location.reload(); };
function enter() {
  $('#auth').hidden = true; $('#app').hidden = false;
  $('#uname').textContent = user.name; $('#avatar').textContent = user.name[0].toUpperCase();
  go('dashboard');
}
document.querySelectorAll('aside nav button').forEach(b => b.onclick = () => go(b.dataset.v));
$('#search').oninput = () => { if (view === 'notes') loadNotes(); if (view === 'cards') loadCards(); };

function go(v) {
  view = v; clearInterval(timerId); timerId = null;
  document.querySelectorAll('aside nav button').forEach(b => b.classList.toggle('on', b.dataset.v === v));
  ({ dashboard: dashView, notes: notesView, cards: cardsView, revise: reviseView })[v]();
}

/* ---------- dashboard ---------- */
async function dashView() {
  const s = await api('api/stats');
  const pct = s.cards ? Math.round(s.known / s.cards * 100) : 0;
  $('#main').innerHTML = `
    <h1>Hello, ${esc(user.name.split(' ')[0])}</h1>
    <div class="stats">
      <div class="stat"><b>${s.notes}</b><span>Notes saved</span></div>
      <div class="stat"><b>${s.cards}</b><span>Flashcards created</span></div>
      <div class="stat"><b>${pct}%</b><span>Flashcards mastered</span><div class="bar"><i style="width:${pct}%"></i></div></div>
    </div>
    <div class="panel"><h2>Focus timer</h2>
      <div class="timer" id="clock">25:00</div>
      <div class="row"><button class="btn" id="tStart">Start 25 min</button><button class="btn ghost" id="tReset">Reset</button></div>
    </div>
    <div class="panel"><h2>Next step</h2>
      <p>${s.notes === 0 ? 'Write your first note, then turn its key points into flashcards.' : (s.cards - s.known) > 0 ? `You have ${s.cards - s.known} flashcards left to master. Start a revision round.` : 'Everything is mastered. Add new flashcards to keep going.'}</p>
      <button class="btn" onclick="go('${s.notes === 0 ? 'notes' : 'revise'}')">${s.notes === 0 ? 'Add a note' : 'Start revising'}</button>
    </div>`;
  const draw = () => $('#clock').textContent = String(Math.floor(secs / 60)).padStart(2, '0') + ':' + String(secs % 60).padStart(2, '0');
  draw();
  $('#tStart').onclick = () => {
    if (timerId) return;
    timerId = setInterval(() => { secs--; draw(); if (secs <= 0) { clearInterval(timerId); timerId = null; secs = 25 * 60; toast('Time is up. Take a 5 minute break.'); draw(); } }, 1000);
  };
  $('#tReset').onclick = () => { clearInterval(timerId); timerId = null; secs = 25 * 60; draw(); };
}

/* ---------- notes ---------- */
function notesView() {
  $('#main').innerHTML = `<h1>Notes</h1>
    <form id="nf" class="panel grid">
      <input name="subject" placeholder="Subject, e.g. Physics">
      <input name="title" placeholder="Note title" required>
      <textarea name="content" placeholder="Write your note here" required></textarea>
      <div><button class="btn">Save note</button></div>
    </form><div id="list"></div>`;
  $('#nf').onsubmit = async e => {
    e.preventDefault();
    if (await act('api/notes', { action: 'add', ...Object.fromEntries(new FormData(e.target)) }, 'Note saved')) { e.target.reset(); loadNotes(); }
  };
  loadNotes();
}
async function loadNotes() {
  const d = await api('api/notes?q=' + encodeURIComponent($('#search').value));
  $('#list').innerHTML = d.length ? d.map(n => `<article class="item"><div>
    <span class="tag">${esc(n.subject || 'General')}</span><h3>${esc(n.title)}</h3><p>${esc(n.content)}</p><small>${esc(n.date)}</small></div>
    <button class="del" aria-label="Delete note" onclick="del('notes',${n.id})">✕</button></article>`).join('')
    : '<p class="empty">No notes found. Add one above to get started.</p>';
}

/* ---------- flashcards ---------- */
function cardsView() {
  $('#main').innerHTML = `<h1>Flashcards</h1>
    <form id="cf" class="panel grid">
      <input name="subject" placeholder="Subject, e.g. Biology">
      <input name="question" placeholder="Question" required>
      <textarea name="answer" placeholder="Answer" required></textarea>
      <div><button class="btn">Add flashcard</button></div>
    </form><div id="list"></div>`;
  $('#cf').onsubmit = async e => {
    e.preventDefault();
    if (await act('api/cards', { action: 'add', ...Object.fromEntries(new FormData(e.target)) }, 'Flashcard added')) { e.target.reset(); loadCards(); }
  };
  loadCards();
}
async function loadCards() {
  const d = await api('api/cards?q=' + encodeURIComponent($('#search').value));
  $('#list').innerHTML = d.length ? d.map(c => `<article class="item ${c.known ? 'done' : ''}"><div>
    <span class="tag">${esc(c.subject || 'General')}</span><h3>${esc(c.question)}</h3><p>${esc(c.answer)}</p>
    <label style="margin:.5rem 0 0;font-weight:600"><input type="checkbox" style="width:auto" ${c.known ? 'checked' : ''} onchange="mark(${c.id},this.checked)"> I know this</label></div>
    <button class="del" aria-label="Delete flashcard" onclick="del('cards',${c.id})">✕</button></article>`).join('')
    : '<p class="empty">No flashcards found. Create one above.</p>';
}
async function mark(id, on) { await act('api/cards', { action: 'known', id, value: on ? 1 : 0 }); loadCards(); }
async function del(type, id) {
  if (!confirm('Delete this item?')) return;
  if (await act('api/' + type, { action: 'delete', id }, 'Deleted')) (type === 'notes' ? loadNotes : loadCards)();
}

/* ---------- revise ---------- */
async function reviseView() {
  deck = (await api('api/cards')).filter(c => !c.known); idx = 0; showCard();
}
function showCard() {
  if (!deck.length) { $('#main').innerHTML = `<h1>Revise</h1><p class="empty">Nothing left to revise. Add flashcards or untick "I know this" on cards you want to practise.</p>`; return; }
  if (idx >= deck.length) {
    $('#main').innerHTML = `<h1>Round complete</h1><p>You went through ${deck.length} cards.</p><button class="btn" onclick="reviseView()">Start another round</button>`; return;
  }
  const c = deck[idx];
  $('#main').innerHTML = `<h1>Revise</h1>
    <p>Card ${idx + 1} of ${deck.length} · <span class="tag">${esc(c.subject || 'General')}</span></p>
    <div class="bar" style="max-width:620px"><i style="width:${idx / deck.length * 100}%"></i></div>
    <div class="flip" id="fl"><div tabindex="0" role="button" aria-label="Flip card">
      <div class="face front">${esc(c.question)}<small>Click to reveal the answer</small></div>
      <div class="face back">${esc(c.answer)}</div></div></div>
    <div class="row"><button class="btn ghost" id="again">Review again later</button><button class="btn" id="got">I know this</button></div>`;
  const fl = $('#fl'); fl.onclick = () => fl.classList.toggle('on');
  fl.onkeydown = e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fl.classList.toggle('on'); } };
  $('#again').onclick = () => { idx++; showCard(); };
  $('#got').onclick = async () => { await act('api/cards', { action: 'known', id: c.id, value: 1 }); idx++; showCard(); };
}

(async () => { try { user = await api('api/auth/me'); enter(); } catch { $('#auth').hidden = false; } })();
