#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const errors = [];
const skipDirs = new Set(['.git', 'target', 'node_modules', 'wiki']);

function walk(dir) {
  const result = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (skipDirs.has(entry.name)) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) result.push(...walk(full));
    else if (entry.name.endsWith('.md')) result.push(full);
  }
  return result;
}

const required = [
  'README.md', 'SECURITY.md', 'CONTRIBUTING.md', 'CODE_OF_CONDUCT.md', 'SUPPORT.md',
  'NOTICE', 'CHANGELOG.md', 'ROADMAP.md', 'GOVERNANCE.md', 'MAINTAINERS.md', 'CONTRIBUTORS.md',
  'docs/index.md', 'docs/getting-started.md', 'docs/configuration.md', 'docs/compatibility.md',
  'docs/migration.md', 'docs/id-selection.md', 'docs/modules.md', 'docs/spi.md',
  'docs/operations.md', 'docs/troubleshooting.md', 'docs/faq.md', 'docs/non-spring.md',
  'docs/architecture.md', 'docs/benchmarks.md', 'docs/benchmark-report.md', 'docs/release.md',
  'docs/design/requirements.md', 'docs/design/adr/README.md', 'docs/third-party.md',
  '.github/ISSUE_TEMPLATE/bug_report.yml', '.github/PULL_REQUEST_TEMPLATE.md',
  '.github/workflows/ci.yml', '.github/workflows/docs.yml', '.github/workflows/docs-check.yml'
];

for (const file of required) {
  if (!fs.existsSync(path.join(root, file))) errors.push('missing required file: ' + file);
}

const config = fs.readFileSync(path.join(root, 'docs/configuration.md'), 'utf8');
const requiredConfigKeys = [
  'knitid.default-type', 'knitid.default-generator', 'knitid.limits.max-batch-size',
  'knitid.health.cache-ttl', 'generators.<name>', 'uuid7.monotonic', 'uuid8.preset',
  'ulid.random-source', 'ksid.worker-id-assigner', 'biz.template', 'knitid.worker.redis',
  'knitid.worker.zookeeper', 'knitid.worker.kubernetes', 'knitid.sequence.redis'
];
for (const key of requiredConfigKeys) {
  if (!config.includes(key)) errors.push('configuration docs missing key: ' + key);
}

const files = walk(root);
for (const file of files) {
  const text = fs.readFileSync(file, 'utf8');
  const rel = path.relative(root, file).replaceAll(path.sep, '/');
  if (text.includes('🚧') || text.includes('P1 骨架')) {
    errors.push(rel + ': contains unfinished status marker');
  }
  const fenceCount = (text.match(/^\s*```/gm) || []).length;
  if (fenceCount % 2 !== 0) errors.push(rel + ': unbalanced code fences');

  const withoutFences = text.replace(/```[\s\S]*?```/g, '');
  const linkPattern = /\[[^\]]*\]\(([^)]+)\)/g;
  let match;
  while ((match = linkPattern.exec(withoutFences)) !== null) {
    let target = match[1].trim();
    if (target.startsWith('<') && target.endsWith('>')) target = target.slice(1, -1);
    if (target.startsWith('http://') || target.startsWith('https://') || target.startsWith('mailto:') || target.startsWith('#')) continue;
    target = target.split('#')[0];
    if (!target) continue;
    try { target = decodeURIComponent(target); } catch { /* keep raw */ }
    const resolved = path.resolve(path.dirname(file), target);
    if (!fs.existsSync(resolved)) errors.push(rel + ' -> broken link: ' + match[1]);
  }
}

if (errors.length) {
  console.error('Documentation check failed:');
  for (const error of errors) console.error(' - ' + error);
  process.exit(1);
}
console.log('Documentation check passed: ' + files.length + ' Markdown files, ' + required.length + ' required files.');
