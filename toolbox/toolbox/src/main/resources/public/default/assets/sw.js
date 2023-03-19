/*
Custom Service worker implementation

in part based on: 

sw-toolbox
https://github.com/GoogleChrome/sw-toolbox
Apache License, Version 2.0
https://github.com/GoogleChrome/sw-toolbox/blob/master/LICENSE
*/

//===========================================================

preCacheList = new Array(
);

//===========================================================

// original (sw-toolbox) util/helper methods
// fetchAndCache is modified!

var scope;

// This is necessary to handle different implementations in the wild....
// The spec defines self.registration, but it was not implemented in Chrome 40.

if (self.registration) {
  scope = self.registration.scope;
} else {
  scope = self.scope || new URL('./', self.location).href;
}

var gCacheName = '$$$sw-cache$$$' + scope;
var gSuccessResponses = new RegExp("([123]\\d\\d)|(40[14567])|410");

function openCache() {
  return caches.open(gCacheName);
}

function cache(url) {
  return openCache().then(function (cache) {
    return cache.add(url);
  });
}

function uncache(url) {
  return openCache().then(function (cache) {
    return cache.delete(url);
  });
}

function renameCache(source, destination) {

  return caches.delete(destination).then(function () {
    return Promise.all([
      caches.open(source),
      caches.open(destination)
    ]).then(function (results) {
      var sourceCache = results[0];
      var destCache = results[1];

      return sourceCache.keys().then(function (requests) {
        return Promise.all(requests.map(function (request) {
          return sourceCache.match(request).then(function (response) {
            return destCache.put(request, response);
          });
        }));
      }).then(function () {
        return caches.delete(source);
      });
    });
  });
}

function cacheFirst(request) {
  return openCache().then(function (cache) {
    return cache.match(request).then(function (response) {
      if (response) {
        return response;
      }

      return fetchAndCache(request);
    });
  });
}

function cacheOnly(request) {
  return openCache().then(function (cache) {
    return cache.match(request);
  });
}

function fastest(request) {

  return new Promise(function (resolve, reject) {

    var rejected = false;
    var reasons = [];

    var maybeReject = function (reason) {

      reasons.push(reason.toString());

      if (rejected) {
        reject(new Error('Both cache and network failed: "' + reasons.join('", "') + '"'));
      } else {
        rejected = true;
      }

    };

    var maybeResolve = function (result) {
      if (result instanceof Response) {
        resolve(result);
      } else {
        maybeReject('No result returned');
      }
    };

    fetchAndCache(request.clone()).then(maybeResolve, maybeReject);

    cacheOnly(request).then(maybeResolve, maybeReject);
  });
}

function networkFirst(request, networkTimeoutSeconds) {

  return openCache().then(function (cache) {

    var timeoutId;
    var promises = [];
    var originalResponse;

    if (networkTimeoutSeconds) {

      var cacheWhenTimedOutPromise = new Promise(function (resolve) {
        timeoutId = setTimeout(function () {
          cache.match(request).then(function (response) {
            if (response) {

              // Only resolve this promise if there's a valid response in the
              // cache. This ensures that we won't time out a network request
              // unless there's a cached entry to fallback on, which is arguably
              // the preferable behavior.

              console.log('sw - networkFirst, timeout', request);

              resolve(response);
            }
          });
        }, networkTimeoutSeconds * 1000);
      });

      promises.push(cacheWhenTimedOutPromise);

    }

    var networkPromise = fetchAndCache(request).then(function (response) {

      // We've got a response, so clear the network timeout if there is one.

      if (timeoutId) {
        clearTimeout(timeoutId);
      }

      if (gSuccessResponses.test(response.status)) {
        return response;
      }

      originalResponse = response;
      throw new Error('Bad response');

    }).catch(function (error) {

      return cache.match(request).then(function (response) {

        // If there's a match in the cache, resolve with that.

        if (response) {
          return response;
        }

        // If we have a Response object from the previous fetch, then resolve
        // with that, even though it corresponds to an error status code.

        if (originalResponse) {
          return originalResponse;
        }

        // If we don't have a Response object from the previous fetch, likely
        // due to a network failure, then reject with the failure error.

        throw error;

      });
    });

    promises.push(networkPromise);

    return Promise.race(promises);
  });
}

function networkOnly(request) {
  return fetch(request);
}

//-----------------------------------------------------------

function fetchAndCache(request) {

  return fetch(request.clone()).then(function (response) {

    // Only cache GET requests with successful responses.
    // Since this is not part of the promise chain, it will be done
    // asynchronously and will not block the response from being returned to the
    // page.

    if (request.method === 'GET' && gSuccessResponses.test(response.status)) {

      // mod.: 204 reponse should be skipped (existing cache item stays, no overwrite...)

      if (response.status == 204) { //TODO: 304 would be more suitable?
        return;
      }

      // ---

      openCache().then(function (cache) {
        cache.put(request, response).then(function () {
          //
        });
      });

    }

    return response.clone();

  });
}

//===========================================================

function inst() {

  // return self.skipWaiting();

  return self.skipWaiting().then(function () {

    // to ensure our service worker takes control of the page as soon as possible

    console.log('sw - install 1 (skipWaiting)');

    // var cacheWhitelist = [];

    return caches.keys().then(function (cacheNames) {

      return Promise.all(cacheNames.map(function (cacheName) {

        // delete old cache (on sw version change)

        if (cacheName.indexOf(gCacheName) > -1) {

          console.log('sw - install 2 (delete old cache): ' + cacheName);
          return caches.delete(cacheName);

        }

      })).then(function () {

        // precache (on sw version change)

        console.log('sw - install 3 (precache): ' + preCacheList);

        return openCache().then(function (cache) {
          return cache.addAll(preCacheList);
        }).catch(function (error) {
          console.log(error);
        });

      });

    });

  });

}

function act() {

  console.log('sw - activate (clients.claim...)');

  // ensure our service worker takes control of the page as soon as possible

  return self.clients.claim();

};

self.addEventListener('install', event => { event.waitUntil(inst()); });
self.addEventListener('activate', event => { event.waitUntil(act()); });

//===========================================================

self.addEventListener('fetch', function (event) {
	
  if (event.request.method === 'GET') {

    var u = event.request.url.toLowerCase();
    
    if (
    (u.indexOf('/cdn/') > -1 && u.indexOf('.jpg') > -1) || 
    (u.indexOf('.cache.js') > -1 && u.indexOf('widgetsets') > -1) || 
	u.indexOf('cdnjs.cloudflare.com') > -1 || 
	u.indexOf('fonts.googleapis.com') > -1 || 
	u.indexOf('fontawesome.com/releases') > -1 || 
	u.endsWith('.woff') || u.endsWith('.woff2') || u.endsWith('.otf')
	) {
	  event.respondWith(cacheFirst(event.request));
    } else {
      event.respondWith(networkOnly(event.request));
    }

  }

});
