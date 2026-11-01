// Шлюз SoundCloud для Cloudflare Workers. Только SoundCloud: Spotify работает без шлюза.
// Публичного ключа API у SoundCloud больше нет, поэтому client_id берём из скриптов его веб-плеера и кэшируем.
// Endpoints: /sc/search?q=<запрос>, /sc/url?id=<id трека>
const BUA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
const CORS = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'Range,Content-Type', 'Access-Control-Expose-Headers': 'Content-Length,Content-Range' };
const SCH = /(^|\.)sndcdn\.com$/;
const J = (o, s = 200) => new Response(JSON.stringify(o), { status: s, headers: { ...CORS, 'Content-Type': 'application/json' } });

let SC_ID = '';
async function scId(force) {
  if (SC_ID && !force) return SC_ID;
  const html = await (await fetch('https://soundcloud.com/', { headers: { 'User-Agent': BUA } })).text();
  const js = [...html.matchAll(/<script[^>]+src="(https:\/\/a-v2\.sndcdn\.com\/assets\/[^"]+\.js)"/g)].map(m => m[1]).reverse();
  for (const u of js) {
    const m = /client_id:"([A-Za-z0-9]{32})"/.exec(await (await fetch(u)).text());
    if (m) return (SC_ID = m[1]);
  }
  throw new Error('не удалось получить client_id SoundCloud');
}
// запрос к api-v2; если ключ устарел, один раз берём новый
async function scGet(path, q = {}) {
  for (const f of [0, 1]) {
    const r = await fetch('https://api-v2.soundcloud.com' + path + '?' + new URLSearchParams({ ...q, client_id: await scId(f) }), { headers: { 'User-Agent': BUA } });
    if (r.ok || f) return r;
  }
}

const scOk = x => x && x.title && x.policy !== 'SNIP' && x.policy !== 'BLOCK' && x.media && x.media.transcodings && x.media.transcodings.length;
const scMap = x => ({
  id: x.id, title: x.title, artist: (x.user && x.user.username) || 'SoundCloud',
  dur: Math.round((x.duration || 0) / 1000),
  cover: (x.artwork_url || (x.user && x.user.avatar_url) || '').replace('-large', '-t500x500')
});

export default {
  async fetch(req) {
    const u = new URL(req.url), p = u.pathname;
    if (req.method === 'OPTIONS') return new Response(null, { headers: CORS });
    try {
      if (p === '/') return J({ ok: 'sc-worker' });
      if (p === '/sc/search') {
        const r = await scGet('/search/tracks', { q: u.searchParams.get('q') || '', limit: '20' });
        if (!r.ok) return J({ error: 'SoundCloud ответил ' + r.status }, 502);
        const j = await r.json();
        return J((j.collection || []).filter(scOk).map(scMap));
      }
      // рекомендации: похожие на трек, а без трека (или если пусто) чарт «в тренде»
      if (p === '/sc/reco') {
        const id = u.searchParams.get('id') || '';
        let l = [];
        if (id) { const r = await scGet('/tracks/' + encodeURIComponent(id) + '/related', { limit: '25' }); if (r.ok) l = (await r.json()).collection || []; }
        if (!l.length) { const r = await scGet('/charts', { kind: 'trending', genre: 'soundcloud:genres:all-music', limit: '25' }); if (r.ok) l = ((await r.json()).collection || []).map(c => c.track); }
        return J(l.filter(scOk).map(scMap));
      }
      // поток идёт через воркер: ссылки CDN подписаны, а у HLS нет CORS
      if (p === '/sc/p' || p === '/sc/h' || p === '/sc/seg') {
        const src = new URL(u.searchParams.get('u') || '');
        if (!SCH.test(src.hostname)) return J({ error: 'forbidden' }, 403);
        const h = { 'User-Agent': BUA };
        if (req.headers.get('Range')) h.Range = req.headers.get('Range');
        const r = await fetch(src, { headers: h });
        if (p !== '/sc/h') {
          const o = { ...CORS, 'Content-Type': r.headers.get('Content-Type') || 'audio/mpeg', 'Accept-Ranges': 'bytes' };
          for (const k of ['Content-Length', 'Content-Range']) if (r.headers.get(k)) o[k] = r.headers.get(k);
          return new Response(r.body, { status: r.status, headers: o });
        }
        const px = x => u.origin + '/sc/seg?u=' + encodeURIComponent(new URL(x, src).href);
        const txt = (await r.text()).split('\n').map(l => /^#EXT-X-(MAP|KEY)/.test(l) ? l.replace(/URI="([^"]+)"/, (_, x) => 'URI="' + px(x) + '"') : l && l[0] !== '#' ? px(l.trim()) : l).join('\n');
        return new Response(txt, { headers: { ...CORS, 'Content-Type': 'application/vnd.apple.mpegurl' } });
      }
      if (p === '/sc/url') {
        const r = await scGet('/tracks/' + encodeURIComponent(u.searchParams.get('id') || ''));
        if (!r.ok) return J({ error: 'SoundCloud ответил ' + r.status }, 502);
        const x = await r.json(), tc = (x.media && x.media.transcodings) || [];
        const pick = tc.find(t => t.format.protocol === 'progressive') || tc.find(t => t.format.protocol === 'hls');
        if (!pick) return J({ error: 'нет потока' }, 404);
        const s = await scGet(pick.url.replace('https://api-v2.soundcloud.com', ''), { track_authorization: x.track_authorization || '' });
        const d = await s.json().catch(() => ({}));
        const hls = pick.format.protocol === 'hls';
        return d.url ? J({ url: u.origin + '/sc/' + (hls ? 'h' : 'p') + '?u=' + encodeURIComponent(d.url), hls }) : J({ error: 'нет ссылки на поток' }, 502);
      }
      return J({ error: 'not found' }, 404);
    } catch (e) {
      return J({ error: String(e) }, 502);
    }
  }
};
