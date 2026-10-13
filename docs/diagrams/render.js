// Renders every .puml in this folder (and activity/) to png/ through the public PlantUML server.
// Usage (from the project root): node docs/diagrams/render.js
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const dir = __dirname;
const alphabet = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_';

// PlantUML's URL encoding: raw deflate, then base64 with its own alphabet
const encode = text => {
  const bytes = zlib.deflateRawSync(Buffer.from(text, 'utf8'), { level: 9 });
  let out = '';
  for (let i = 0; i < bytes.length; i += 3) {
    const [b1, b2 = 0, b3 = 0] = bytes.subarray(i, i + 3);
    out += [b1 >> 2, ((b1 & 3) << 4) | (b2 >> 4), ((b2 & 15) << 2) | (b3 >> 6), b3 & 63].map(n => alphabet[n]).join('');
  }
  return out;
};

const files = ['.', 'activity'].flatMap(d => fs.existsSync(path.join(dir, d))
  ? fs.readdirSync(path.join(dir, d)).filter(f => f.endsWith('.puml')).map(f => path.join(dir, d, f)) : []);
fs.mkdirSync(path.join(dir, 'png'), { recursive: true });

(async () => {
  for (const file of files) {
    const res = await fetch('https://www.plantuml.com/plantuml/png/' + encode(fs.readFileSync(file, 'utf8')));
    const out = path.join(dir, 'png', path.basename(file, '.puml') + '.png');
    fs.writeFileSync(out, Buffer.from(await res.arrayBuffer()));
    console.log(res.ok ? 'ok   ' : 'ERROR', path.relative(dir, out));
  }
})();
