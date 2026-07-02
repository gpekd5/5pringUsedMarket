import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import http from 'node:http';
import https from 'node:https';

function s3UploadProxyPlugin() {
  return {
    name: 's3-upload-proxy',
    configureServer(server) {
      server.middlewares.use('/__s3-upload', (request, response, next) => {
        if (request.method !== 'PUT') {
          next();
          return;
        }

        const requestUrl = new URL(request.url, 'http://localhost');
        const target = requestUrl.searchParams.get('target');

        if (!target) {
          response.statusCode = 400;
          response.end('Missing S3 upload target.');
          return;
        }

        let targetUrl;

        try {
          targetUrl = new URL(target);
        } catch {
          response.statusCode = 400;
          response.end('Invalid S3 upload target.');
          return;
        }

        const requestModule = targetUrl.protocol === 'http:' ? http : https;
        const proxyHeaders = {};

        if (request.headers['content-type']) {
          proxyHeaders['content-type'] = request.headers['content-type'];
        }

        if (request.headers['content-length']) {
          proxyHeaders['content-length'] = request.headers['content-length'];
        }

        const proxyRequest = requestModule.request(
          targetUrl,
          {
            method: 'PUT',
            headers: proxyHeaders,
          },
          (proxyResponse) => {
            response.statusCode = proxyResponse.statusCode ?? 502;
            proxyResponse.pipe(response);
          },
        );

        proxyRequest.on('error', (error) => {
          if (!response.headersSent) {
            response.statusCode = 502;
          }
          response.end(error.message);
        });

        request.pipe(proxyRequest);
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), tailwindcss(), s3UploadProxyPlugin()],
  define: {
    global: 'globalThis',
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
  },
});
