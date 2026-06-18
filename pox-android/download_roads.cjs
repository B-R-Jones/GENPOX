const https = require('https');
const fs = require('fs');
const path = require('path');
const querystring = require('querystring');

const lat = 39.1710563;
const lng = -94.4636246;
const radius = 15000; // 15km radius

// Fetch major, secondary, tertiary, and residential roads
const query = `[out:json];(way(around:${radius},${lat},${lng})[highway~"motorway|trunk|primary|secondary|tertiary|unclassified|residential"];);out geom;`;
const postData = querystring.stringify({ data: query });

const targetFile = path.join(__dirname, 'app', 'src', 'main', 'assets', 'pre_cached_roads.json');

// Ensure directory exists
const dir = path.dirname(targetFile);
if (!fs.existsSync(dir)){
    fs.mkdirSync(dir, { recursive: true });
}

console.log(`Sending query for 15km radius around lat=${lat}, lng=${lng}...`);
console.log(`Target file: ${targetFile}`);

const options = {
    hostname: 'overpass-api.de',
    path: '/api/interpreter',
    method: 'POST',
    headers: {
        'User-Agent': 'GenPoxRadar/1.0 (brent@example.com)',
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(postData)
    }
};

const req = https.request(options, (res) => {
    console.log('Status Code:', res.statusCode);
    if (res.statusCode !== 200) {
        console.error('Failed to get 200 status code');
        let errorData = '';
        res.on('data', (c) => errorData += c);
        res.on('end', () => {
            console.error('Error body:', errorData);
            process.exit(1);
        });
        return;
    }

    const fileStream = fs.createWriteStream(targetFile);
    res.pipe(fileStream);

    fileStream.on('finish', () => {
        fileStream.close();
        console.log(`Success! Pre-cached roads saved. File size: ${fs.statSync(targetFile).size} bytes`);
        process.exit(0);
    });
});

req.on('error', (err) => {
    console.error('Request Error:', err.message);
    process.exit(1);
});

req.write(postData);
req.end();
