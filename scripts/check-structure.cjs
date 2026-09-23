const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const walk = directory => fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => entry.isDirectory() ? walk(path.join(directory, entry.name)) : [path.join(directory, entry.name)]);
const javaRoot = path.join(root, 'backend/src/main/java');
const javaFiles = walk(javaRoot).filter(file => file.endsWith('.java'));
const knownTypes = new Set();
for (const file of javaFiles) {
  const source = fs.readFileSync(file, 'utf8');
  const pkg = source.match(/^package ([\w.]+);/m)?.[1];
  assert.equal(pkg, path.relative(javaRoot, path.dirname(file)).split(path.sep).join('.'), `Package mismatch: ${file}`);
  if (path.basename(file) === 'package-info.java') continue;
  const types = [...source.matchAll(/^public (?:(?:abstract|final) )?(?:class|interface|record|enum) (\w+)/gm)];
  assert.equal(types.length, 1, `Expected one public type: ${file}`);
  assert.equal(types[0][1], path.basename(file, '.java'));
  knownTypes.add(`${pkg}.${types[0][1]}`);
}
for (const file of javaFiles) {
  for (const match of fs.readFileSync(file, 'utf8').matchAll(/^import (com\.binava\.stafffinance\.[\w.]+);/gm)) {
    assert(knownTypes.has(match[1]), `Unresolved application import ${match[1]} in ${file}`);
  }
}
const frontRoot = path.join(root, 'frontend/src');
const frontFiles = walk(frontRoot).filter(file => /\.(js|jsx)$/.test(file));
for (const file of frontFiles) {
  const source = fs.readFileSync(file, 'utf8');
  for (const match of source.matchAll(/(?:from\s*|import\s*)["'](\.[^"']+)["']/g)) {
    const target = path.resolve(path.dirname(file), match[1]);
    assert([target, target + '.js', target + '.jsx', path.join(target, 'index.js'), path.join(target, 'index.jsx')].some(p => fs.existsSync(p) && fs.statSync(p).isFile()), `Unresolved frontend import ${match[1]} in ${file}`);
  }
}
console.log(`Structure verified: ${knownTypes.size} Java types and ${frontFiles.length} frontend modules.`);
