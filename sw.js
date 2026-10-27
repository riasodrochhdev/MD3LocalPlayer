const C='mp-v5',F=['./','./index.html','./manifest.webmanifest','./icon-192.png','./icon-512.png'];
self.addEventListener('install',e=>e.waitUntil(caches.open(C).then(c=>c.addAll(F)).then(()=>self.skipWaiting())));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!==C).map(x=>caches.delete(x)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{const r=e.request,u=new URL(r.url),own=u.origin===location.origin,font=/^fonts\.(googleapis|gstatic)\.com$/.test(u.hostname);
 if(r.method!=='GET'||!(own||font))return;
 if(r.mode==='navigate'){e.respondWith(Promise.race([fetch(r).then(x=>{if(x.ok)caches.open(C).then(c=>c.put('./index.html',x.clone()));return x}),new Promise((_,j)=>setTimeout(j,3500))]).catch(()=>caches.match('./index.html')));return}
 e.respondWith(caches.match(r).then(h=>{const n=fetch(r).then(x=>{if(x&&(x.ok||x.type==='opaque'))caches.open(C).then(c=>c.put(r,x.clone()));return x}).catch(()=>h);return h||n}))});
