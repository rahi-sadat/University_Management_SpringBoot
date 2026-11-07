// Regenerate the importable collection with: node scripts/generate-postman.cjs
const fs = require('node:fs');
const path = require('node:path');
const items = [];
let folder;
function group(name) { folder = { name, item: [] }; items.push(folder); }
function request(name, method, route, status, body, options = {}) {
  const test = [`pm.test('HTTP ${status}', () => pm.response.to.have.status(${status}));`];
  if (status >= 400) test.push("pm.test('Safe JSON error', () => pm.expect(pm.response.json().error).to.be.a('string'));");
  if (status === 201) test.push("pm.test('Location header', () => pm.expect(pm.response.headers.get('Location')).to.include('/' + pm.response.json().id));");
  if (options.capture) test.push(`pm.collectionVariables.set('${options.capture}', pm.response.json().id);`);
  if (options.test) test.push(options.test);
  const entry = { name, request: { method, header: [], url: '{{baseUrl}}' + route },
    event: [{ listen: 'test', script: { type: 'text/javascript', exec: test } }] };
  if (body !== undefined) {
    entry.request.header.push({ key: 'Content-Type', value: 'application/json' });
    entry.request.body = { mode: 'raw', raw: typeof body === 'string' ? body : JSON.stringify(body, null, 2), options: { raw: { language: 'json' } } };
  }
  if (options.noauth || options.header) entry.request.auth = { type: 'noauth' };
  if (options.header) entry.request.header.push({ key: 'Authorization', value: options.header });
  if (options.pre) entry.event.unshift({ listen: 'prerequest', script: { type: 'text/javascript', exec: [options.pre] } });
  folder.item.push(entry);
}
const payload = (kind, name = 'Test Person') => ({ fullName: name, email: `{{runId}}-${kind}@test.local`, password: '{{testPassword}}' });
function login(name, email, password, role, extra = '') {
  request(name, 'POST', '/api/auth/login', 200, { email, password }, { noauth: true,
    test: `const result = pm.response.json();
pm.test('Bearer token issued', () => { pm.expect(result.accessToken).to.be.a('string'); pm.expect(result.tokenType).to.eql('Bearer'); pm.expect(result.role).to.eql('${role}'); pm.expect(result.expiresIn).to.be.above(0); });
pm.collectionVariables.set('accessToken', result.accessToken);
${extra}` });
}
function crud(kind, id, label) {
  request(`${label}: create`, 'POST', `/api/${kind}`, 201, payload(label, label), { capture: id });
  request(`${label}: list`, 'GET', `/api/${kind}`, 200);
  request(`${label}: get`, 'GET', `/api/${kind}/{{${id}}}`, 200);
  request(`${label}: update`, 'PUT', `/api/${kind}/{{${id}}}`, 200, payload(label, 'Updated Person'),
    { test: "pm.test('Updated name', () => pm.expect(pm.response.json().fullName).to.eql('Updated Person'));" });
}
function forbidden(kind, id, actor) {
  for (const [method, suffix] of [['GET', ''], ['GET', `/{{${id}}}`], ['POST', ''], ['PUT', `/{{${id}}}`], ['DELETE', `/{{${id}}}`]]) {
    request(`${actor}: ${method} ${kind}${suffix ? ' by ID' : ''} forbidden`, method, `/api/${kind}${suffix}`, 403,
      ['POST', 'PUT'].includes(method) ? payload('forbidden') : undefined);
  }
}
group('01 Admin login and CRUD');
login('Login as configured administrator', '{{adminEmail}}', '{{adminPassword}}', 'ADMIN',
  "pm.collectionVariables.set('runId', 'ums-' + Date.now() + '-' + Math.random().toString(36).slice(2, 9)); pm.collectionVariables.set('testPassword', pm.variables.replaceIn('{{$guid}}') + 'Aa!1');",
);
for (const kind of ['admins', 'teachers', 'students']) crud(kind, kind + 'Id', kind);
request('Duplicate email on create', 'POST', '/api/students', 409, payload('admins'));
request('Duplicate email on update', 'PUT', '/api/teachers/{{teachersId}}', 409, payload('admins'));
request('Invalid fields', 'POST', '/api/students', 400, { fullName: '', email: 'invalid', password: 'short' });
request('Malformed JSON', 'POST', '/api/students', 400, '{');
request('Invalid ID', 'GET', '/api/students/0', 400);
for (const method of ['GET', 'PUT', 'DELETE']) request(`Wrong resource type: ${method}`, method, '/api/students/{{teachersId}}', 404, method === 'PUT' ? payload('wrong') : undefined);
request('Client role cannot assign privileges', 'POST', '/api/students', 400, { ...payload('role'), role: 'ADMIN' });

group('02 Invalid authentication');
request('Incorrect credentials', 'POST', '/api/auth/login', 401, { email: '{{adminEmail}}', password: '{{testPassword}}' }, { noauth: true });
request('Missing token', 'GET', '/api/students', 401, undefined, { noauth: true });
request('Missing token on mutation', 'POST', '/api/students', 401, payload('anonymous'), { noauth: true });
request('Malformed token', 'GET', '/api/students', 401, undefined, { header: 'Bearer this-is-not-a-jwt' });
request('Malformed bearer header', 'GET', '/api/students', 401, undefined, { header: 'Token {{accessToken}}' });
request('Tampered token', 'GET', '/api/students', 401, undefined, { header: 'Bearer {{tamperedToken}}',
  pre: "const token = pm.collectionVariables.get('accessToken'); const i = token.lastIndexOf('.') + 1; pm.collectionVariables.set('tamperedToken', token.slice(0, i) + (token[i] === 'a' ? 'b' : 'a') + token.slice(i + 1));" });

group('03 Teacher permissions');
login('Login as created teacher', '{{runId}}-teachers@test.local', '{{testPassword}}', 'TEACHER');
forbidden('admins', 'adminsId', 'Teacher');
forbidden('students', 'studentsId', 'Teacher');
crud('teachers', 'teacherCrudId', 'teacher-crud');
request('Teacher deletes teacher', 'DELETE', '/api/teachers/{{teacherCrudId}}', 204);
request('Deleted teacher is missing', 'GET', '/api/teachers/{{teacherCrudId}}', 404);
request('Teacher logs out', 'POST', '/api/auth/logout', 200);
request('Revoked teacher token', 'GET', '/api/teachers', 401);
request('Repeated logout is safe', 'POST', '/api/auth/logout', 401);

group('04 Student permissions');
login('Login as created student', '{{runId}}-students@test.local', '{{testPassword}}', 'STUDENT');
forbidden('admins', 'adminsId', 'Student');
forbidden('teachers', 'teachersId', 'Student');
crud('students', 'studentCrudId', 'student-crud');
request('Student deletes student', 'DELETE', '/api/students/{{studentCrudId}}', 204);
request('Deleted student is missing', 'GET', '/api/students/{{studentCrudId}}', 404);
request('Student logs out', 'POST', '/api/auth/logout', 200);
request('Revoked student token', 'GET', '/api/students', 401);

group('05 Cleanup and logout');
login('Administrator logs in to clean up', '{{adminEmail}}', '{{adminPassword}}', 'ADMIN');
for (const kind of ['admins', 'teachers', 'students']) {
  request(`Administrator deletes ${kind}`, 'DELETE', `/api/${kind}/{{${kind}Id}}`, 204);
  for (const method of ['GET', 'PUT', 'DELETE']) request(`Missing ${kind}: ${method}`, method, `/api/${kind}/{{${kind}Id}}`, 404, method === 'PUT' ? payload('deleted') : undefined);
}
request('Administrator logs out', 'POST', '/api/auth/logout', 200);
request('Revoked administrator token', 'GET', '/api/admins', 401);
request('Unauthenticated after logout', 'GET', '/api/students', 401, undefined, { noauth: true });

group('Expiration');
folder.description = 'For a dedicated test instance with JWT_EXPIRES_IN=2, run this folder alone. On the normal 3600-second instance the expiration request is skipped; automated Java tests always cover expired tokens.';
login('Obtain a short-lived token', '{{adminEmail}}', '{{adminPassword}}', 'ADMIN',
  "pm.collectionVariables.set('expirationSeconds', result.expiresIn); if (result.expiresIn <= 5) { setTimeout(() => {}, (result.expiresIn + 1) * 1000); }");
request('Expired signed token', 'GET', '/api/students', 401, undefined, {
  pre: "if (Number(pm.collectionVariables.get('expirationSeconds')) > 5) { console.log('Expiration check skipped: run this folder on a dedicated JWT_EXPIRES_IN=2 instance.'); pm.execution.skipRequest(); }" });

const collection = {
  info: { name: 'University Management System - JWT', schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
    description: 'Run folders 01-05 in order. Set baseUrl, adminEmail and adminPassword in a private environment. Creates unique test accounts and deletes them. Bearer tokens only. Expiration is a separate short-lived-server check.' },
  auth: { type: 'bearer', bearer: [{ key: 'token', value: '{{accessToken}}', type: 'string' }] },
  variable: [{ key: 'accessToken', value: '' }], item: items,
  event: [{ listen: 'test', script: { type: 'text/javascript', exec: [
    "pm.test('No authentication cookies', () => pm.expect(pm.response.headers.has('Set-Cookie')).to.eql(false));",
    "pm.test('No password or hash leakage', () => { const text = pm.response.text(); pm.expect(text).not.to.match(/\"password(?:Hash)?\"\\s*:/i); pm.expect(text).not.to.match(/\\$2[aby]\\$/); });",
    "if (pm.response.code === 204) pm.test('Empty DELETE response', () => pm.expect(pm.response.text()).to.eql(''));"
  ] } }]
};
const environment = { name: 'University Management - Local', values: [
  { key: 'baseUrl', value: 'http://localhost:8080', enabled: true },
  { key: 'adminEmail', value: 'admin@university.local', enabled: true },
  { key: 'adminPassword', value: '', enabled: true, type: 'secret' }
], _postman_variable_scope: 'environment' };
const destination = path.join(__dirname, '..', 'postman');
fs.mkdirSync(destination, { recursive: true });
fs.writeFileSync(path.join(destination, 'University-Management-System.postman_collection.json'), JSON.stringify(collection, null, 2) + '\n');
fs.writeFileSync(path.join(destination, 'University-Management-System.postman_environment.json'), JSON.stringify(environment, null, 2) + '\n');
console.log(`Generated collection: ${items.reduce((count, item) => count + item.item.length, 0)} requests`);
