async function testOSM() {
    const lat = 37.7749;
    const lng = -122.4194;
    const query = `[out:json];(way(around:2000,${lat},${lng})[highway~"motorway|trunk|primary|secondary|tertiary|unclassified|residential"];way(around:10000,${lat},${lng})[highway~"motorway|trunk|primary"];);out geom;`;
    
    const endpoints = [
        "https://overpass.osm.ch/api/interpreter",
        "https://overpass.openstreetmap.fr/api/interpreter",
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    ];

    for (const url of endpoints) {
        try {
            console.log(`Querying ${url}...`);
            const res = await fetch(url, {
                method: 'POST',
                headers: {
                    'User-Agent': 'GenPoxRadar/1.0 (brent@example.com)',
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json'
                },
                body: 'data=' + encodeURIComponent(query)
            });
            console.log(`Status: ${res.status}`);
            if (res.status === 200) {
                const data = await res.json();
                console.log(`Success! Found ${data.elements?.length || 0} elements.`);
                if (data.elements && data.elements.length > 0) {
                    console.log(`Sample element:`, JSON.stringify(data.elements[0], null, 2));
                    break;
                }
            }
        } catch (e) {
            console.error(`Error for ${url}:`, e.message);
        }
    }
}

testOSM();
