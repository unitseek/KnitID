#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const errors = [];
const skipDirs = new Set(['.git', 'target', 'node_modules', 'wiki', 'ocr-fix']);

function walk(dir) {
  const result = [];
  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch (error) {
    errors.push('cannot read directory ' + path.relative(root, dir) + ': ' + error.message);
    return result;
  }
  for (const entry of entries) {
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

const configPath = path.join(root, 'docs/configuration.md');
let config = '';
if (fs.existsSync(configPath)) {
  try {
    config = fs.readFileSync(configPath, 'utf8');
  } catch (error) {
    errors.push('cannot read docs/configuration.md: ' + error.message);
  }
}
const requiredConfigKeys = [
  'knitid.default-type', 'knitid.default-generator', 'knitid.limits.max-batch-size',
  'knitid.health.cache-ttl', 'generators.<name>', 'uuid7.monotonic', 'uuid8.preset',
  'ulid.random-source', 'ksid.worker-id-assigner', 'biz.template', 'ksuid.random-source', 'typeid.prefixes', 'typeid.default-prefix', 'typeid.allow-empty-prefix', 'knitid.worker.redis',
  'knitid.worker.zookeeper', 'knitid.worker.kubernetes', 'knitid.sequence.redis'
];
if (config) {
  for (const key of requiredConfigKeys) {
    if (!config.includes(key)) errors.push('configuration docs missing key: ' + key);
  }
}

const files = walk(root);
for (const file of files) {
  let text;
  try {
    text = fs.readFileSync(file, 'utf8');
  } catch (error) {
    errors.push(path.relative(root, file) + ': cannot read: ' + error.message);
    continue;
  }
  const rel = path.relative(root, file).replaceAll(path.sep, '/');
  if (text.includes('🚧') || text.includes('P1 骨架')) {
    errors.push(rel + ': contains unfinished status marker');
  }
  const fenceCount = (text.match(/^\s*(?:```|~~~)/gm) || []).length;
  if (fenceCount % 2 !== 0) errors.push(rel + ': unbalanced code fences');

  const withoutFences = text.replace(/```[\s\S]*?```|~~~[\s\S]*?~~~/g, '');
  const withoutInlineCode = withoutFences.replace(/`[^`]*`/g, '');
  const linkPattern = /\[[^\]]*\]\(([^)]+)\)/g;
  let match;
  while ((match = linkPattern.exec(withoutInlineCode)) !== null) {
    let target = match[1].trim();
    const titled = target.match(/^(\S+)\s+["'][^"']*["']$/);
    if (titled) target = titled[1];
    if (target.startsWith('<') && target.endsWith('>')) target = target.slice(1, -1);
    if (target.startsWith('http://') || target.startsWith('https://') || target.startsWith('mailto:') || target.startsWith('#')) continue;
    target = target.split('#')[0];
    if (!target) continue;
    try { target = decodeURIComponent(target); } catch { /* keep raw */ }
    if (path.isAbsolute(target)) {
      errors.push(rel + ' -> absolute link is not allowed: ' + match[1]);
      continue;
    }
    const resolved = path.resolve(path.dirname(file), target);
    const relative = path.relative(root, resolved);
    if (relative.startsWith('..' + path.sep) || relative === '..' || path.isAbsolute(relative)) {
      errors.push(rel + ' -> link escapes repository: ' + match[1]);
      continue;
    }
    if (!fs.existsSync(resolved)) errors.push(rel + ' -> broken link: ' + match[1]);
  }
}

if (errors.length) {
  console.error('Documentation check failed:');
  for (const error of errors) console.error(' - ' + error);
  process.exit(1);
}
console.log('Documentation check passed: ' + files.length + ' Markdown files, ' + required.length + ' required files.');
