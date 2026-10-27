const C='mp-v1',F=['./','./index.html','./manifest.webmanifest','./icon-192.png','./icon-512.png'];
self.addEventListener('install',e=>e.waitUntil(caches.open(C).then(c=>c.addAll(F)).then(()=>self.skipWaiting())));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!==C).map(x=>caches.delete(x)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{const r=e.request,u=new URL(r.url);
 if(r.method!=='GET'||!(u.origin===location.origin||/(^|\.)(googleapis|gstatic)\.com$/.test(u.hostname)))return;
 e.respondWith(caches.match(r,{ignoreSearch:true}).then(h=>{
  const n=fetch(r).then(x=>{if(x&&(x.ok||x.type==='opaque'))caches.open(C).then(c=>c.put(r,x.clone()));return x}).catch(()=>h||(r.mode==='navigate'?caches.match('./index.html'):undefined));
  return h||n}))});
