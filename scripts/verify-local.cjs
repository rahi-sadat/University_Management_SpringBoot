// Verify the packaged application against configured local PostgreSQL without exposing credentials.
// Creates/deletes only the collection's uniquely named test accounts. Never drops database data.
const fs = require('node:fs');
const path = require('node:path');
const { spawn } = require('node:child_process');
const assert = require('node:assert/strict');
const newman = require('../target/postman-tools/node_modules/newman');
const root = path.resolve(__dirname, '..');
const config = {};
for (const line of fs.readFileSync(path.join(root, '.env'), 'utf8').split(/\r?\n/)) {
  const match = line.match(/^([A-Z][A-Z0-9_]*)=(.*)$/);
  if (match) config[match[1]] = match[2].trim().replace(/^(['"])(.*)\1$/, '$2');
}
const env = {
  ...process.env,
  SPRING_DATASOURCE_URL: process.env.SPRING_DATASOURCE_URL || `jdbc:postgresql://localhost:5432/${config.DB_NAME || 'university_management'}`,
  SPRING_DATASOURCE_USERNAME: process.env.SPRING_DATASOURCE_USERNAME || config.DB_USERNAME || 'university_user',
  SPRING_DATASOURCE_PASSWORD: process.env.SPRING_DATASOURCE_PASSWORD || config.DB_PASSWORD,
  APP_BOOTSTRAP_ADMIN_EMAIL: process.env.APP_BOOTSTRAP_ADMIN_EMAIL || config.ADMIN_EMAIL || 'admin@university.local',
  APP_BOOTSTRAP_ADMIN_PASSWORD: process.env.APP_BOOTSTRAP_ADMIN_PASSWORD || config.ADMIN_PASSWORD,
  JWT_SECRET: process.env.JWT_SECRET || config.JWT_SECRET,
  JWT_EXPIRES_IN: '3600', SERVER_PORT: '0', DEBUG: 'false', LOGGING_LEVEL_ROOT: 'INFO',
  LOGGING_LEVEL_ORG_SPRINGFRAMEWORK: 'INFO', LOGGING_LEVEL_ORG_HIBERNATE: 'INFO'
};
const jar = path.resolve(root, process.argv[2] || 'target/university-management-0.0.1-SNAPSHOT.jar');
const log = fs.createWriteStream(path.join(root, 'target/local-verification-app.log'));
let child;
async function start() {
  child = spawn('java', ['-jar', jar], { cwd: root, env, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  return new Promise((resolve, reject) => {
    let buffered = '';
    const timer = setTimeout(() => reject(new Error('Startup timed out; inspect the private target/local-verification-app.log')), 120000);
    child.on('error', () => { clearTimeout(timer); reject(new Error('Could not launch Java')); });
    child.on('exit', () => { clearTimeout(timer); reject(new Error('Application stopped before startup; inspect target/local-verification-app.log')); });
    child.stderr.on('data', data => log.write(data));
    child.stdout.on('data', data => {
      log.write(data);
      buffered = (buffered + data.toString()).slice(-12000);
      const match = buffered.match(/Tomcat started on port (\d+)/);
      const administratorReady = /Initial administrator created successfully\.|Administrator already exists\./.test(buffered);
      if (match && administratorReady) { clearTimeout(timer); resolve(`http://localhost:${match[1]}`); }
    });
  });
}
async function stop() {
  if (!child || child.exitCode !== null) return;
  const running = child;
  await new Promise(resolve => { running.once('exit', resolve); running.kill(); });
  child = undefined;
}
async function call(base, route, token, body) {
  return fetch(base + route, { method: body !== undefined || route.endsWith('logout') ? 'POST' : 'GET',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(10000) });
}
async function login(base) {
  const response = await call(base, '/api/auth/login', null,
    { email: env.APP_BOOTSTRAP_ADMIN_EMAIL, password: env.APP_BOOTSTRAP_ADMIN_PASSWORD });
  assert.equal(response.status, 200, 'Configured administrator must authenticate');
  return (await response.json()).accessToken;
}
async function main() {
  let base = await start();
  const collection = JSON.parse(fs.readFileSync(path.join(root, 'postman/University-Management-System.postman_collection.json'), 'utf8'));
  const summary = await new Promise((resolve, reject) => newman.run({ collection,
    environment: { values: [
      { key: 'baseUrl', value: base, enabled: true },
      { key: 'adminEmail', value: env.APP_BOOTSTRAP_ADMIN_EMAIL, enabled: true },
      { key: 'adminPassword', value: env.APP_BOOTSTRAP_ADMIN_PASSWORD, enabled: true }
    ] }, reporters: [], timeoutRequest: 30000, timeout: 300000 }, (error, result) => error ? reject(new Error('Newman could not complete')) : resolve(result)));
  const failures = summary.run.failures.map(failure => ({ request: failure.source.name,
    error: failure.error.name, timeout: /timeout|timedout/i.test(failure.error.message || '') }));
  const counts = { requests: summary.run.stats.requests.total, assertions: summary.run.stats.assertions.total, failures };
  fs.writeFileSync(path.join(root, 'target/local-verification-summary.json'), JSON.stringify(counts, null, 2));
  console.log(JSON.stringify(counts));
  assert.equal(failures.length, 0, 'PostgreSQL collection checks must pass');
  const revoked = await login(base);
  const active = await login(base);
  assert.equal((await call(base, '/api/auth/logout', revoked)).status, 200);
  await stop();
  base = await start();
  assert.equal((await call(base, '/api/students', revoked)).status, 401, 'Revocation must survive application restart');
  assert.equal((await call(base, '/api/students', active)).status, 200, 'Same signing key must validate unrevoked tokens after restart');
  assert.equal((await call(base, '/api/auth/logout', active)).status, 200);
  console.log('PostgreSQL restart checks passed: revoked token rejected, active token accepted.');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; })
  .finally(async () => { await stop(); log.end(); });
