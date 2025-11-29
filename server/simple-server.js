const express = require('express');

const app = express();
const PORT = process.env.PORT || 3000;

async function versionMiddleware(req, res, next) {
  try {
    const latestVersion = await getLatestVersionCached();
    req.latestClientVersion = latestVersion || "unknown";
  } catch (err) {
    console.error("Failed to fetch latest client version:", err);
    req.latestClientVersion = "unknown";
  }
  next();
}

// Middleware
app.use(express.json());
app.use(versionMiddleware);

// In-memory storage for airport data
const airportData = {};

// Helper function to validate ICAO codes (4 letters)
function isValidICAO(icao) {
  return /^[A-Z]{4}$/.test(icao.toUpperCase());
}

async function fetchLatestClientVersion() {
  const url = "https://api.github.com/repos/EvenAR/aman-dman/releases/latest";
  const token = process.env.GITHUB_TOKEN;

  const response = await fetch(url, {
    headers: {
      "Accept": "application/vnd.github+json",
      "Authorization": `Bearer ${token}`,
      "X-GitHub-Api-Version": "2022-11-28"
    }
  });

  if (!response.ok) {
    console.error("GitHub API error:", response.status);
    return null;
  }

  const json = await response.json();
  const tag = json.tag_name; // e.g. "v1.4.2"

  // Remove leading "v" if present
  return tag.startsWith("v") ? tag.slice(1) : tag;
}

let cache = {
  version: null,
  timestamp: 0
};

async function getLatestVersionCached() {
  const ttlMs = 10 * 60 * 1000; // 10 minutes
  const now = Date.now();

  if (cache.version && (now - cache.timestamp < ttlMs)) {
    return cache.version;
  }

  const latest = await fetchLatestClientVersion();
  if (latest) {
    cache = { version: latest, timestamp: now };
  }

  return cache.version;
}

// Generic handler for GET endpoints
function getDataHandler(dataType, defaultValue = null) {
  return (req, res) => {
    const icao = req.params.icao.toUpperCase();
    const sessionId = req.headers['x-session-uuid'];
    
    if (!isValidICAO(icao)) {
      return res.status(400).json({ error: 'Invalid ICAO code' });
    }
    
    // Update client activity if session ID is provided
    if (sessionId) {
      updateClientActivity(sessionId);
    }
    
    const data = airportData[icao]?.[dataType] || defaultValue;
    res.json(data);
  };
}

// Generic handler for POST endpoints
function postDataHandler(dataType) {
  return (req, res) => {
    const icao = req.params.icao.toUpperCase();
    const sessionId = req.headers['x-session-uuid'];
    
    if (!isValidICAO(icao)) {
      return res.status(400).json({ error: 'Invalid ICAO code' });
    }
    
    // Update client activity if session ID is provided
    if (sessionId) {
      updateClientActivity(sessionId);
    }
    
    // Initialize airport data if it doesn't exist
    if (!airportData[icao]) {
      airportData[icao] = {};
    }
    
    // Overwrite the data completely
    airportData[icao][dataType] = req.body;
    
    res.json({ 
      message: `${dataType} data stored successfully`,
      data: airportData[icao][dataType] 
    });
  };
}

const masterRoles = {}; // ICAO -> session UUID
const heartbeats = {}; // Session UUID -> timestamp (only for master roles)
const sessionStartTimes = {}; // Session UUID -> start timestamp (only for master roles)
const clientActivity = {}; // Session UUID -> last activity timestamp (for all clients)
const clientStartTimes = {}; // Session UUID -> start timestamp (for all clients)

// Clean up old heartbeats (older than 1 minute) - only affects master roles
function cleanupOldHeartbeats() {
  const now = Date.now();
  const oneMinute = 60000; // 1 minute in milliseconds
  
  Object.keys(heartbeats).forEach(sessionId => {
    if (now - heartbeats[sessionId] > oneMinute) {
      delete heartbeats[sessionId];
      delete sessionStartTimes[sessionId]; // Also clean up start times
      
      // Also remove from master roles if this session was a master
      Object.keys(masterRoles).forEach(icao => {
        if (masterRoles[icao] === sessionId) {
          delete masterRoles[icao];
        }
      });
    }
  });
}

// Clean up old client activity (older than 5 minutes)
function cleanupOldClientActivity() {
  const now = Date.now();
  const fiveMinutes = 300000; // 5 minutes in milliseconds
  
  Object.keys(clientActivity).forEach(sessionId => {
    if (now - clientActivity[sessionId] > fiveMinutes) {
      delete clientActivity[sessionId];
      delete clientStartTimes[sessionId];
    }
  });
}

// Run cleanup every 30 seconds
setInterval(cleanupOldHeartbeats, 30000);
setInterval(cleanupOldClientActivity, 30000);

function acquireMasterRole(icao, sessionId) {
  const currentMaster = masterRoles[icao];
  const currentMasterHeartbeat = heartbeats[currentMaster];

  const now = Date.now();

  // If there's a current master, check heartbeat
  if (currentMaster) {
    if (now - currentMasterHeartbeat < 30000) { // 30 seconds timeout
      return false; // Current master is still active
    }
  }

  // Assign new master role
  masterRoles[icao] = sessionId;
  heartbeats[sessionId] = now;
  
  // Track session start time if this is the first time we see this session as master
  if (!sessionStartTimes[sessionId]) {
    sessionStartTimes[sessionId] = now;
  }
  
  return true;
}

// Update heartbeat for a session (only for master roles)
function updateHeartbeat(sessionId) {
  if (sessionId) {
    const now = Date.now();
    heartbeats[sessionId] = now;
    
    // Track session start time if this is the first time we see this session as master
    if (!sessionStartTimes[sessionId]) {
      sessionStartTimes[sessionId] = now;
    }
  }
}

// Update client activity (for all API calls)
function updateClientActivity(sessionId) {
  if (sessionId) {
    const now = Date.now();
    clientActivity[sessionId] = now;
    
    // Track client start time if this is the first time we see this client
    if (!clientStartTimes[sessionId]) {
      clientStartTimes[sessionId] = now;
    }
  }
}

// Log connected clients and their connection times
function logConnectedClients() {
  const activeClientsCount = Object.keys(clientActivity).length;
  const masterSessionsCount = Object.keys(heartbeats).length;
  
  if (activeClientsCount === 0 && masterSessionsCount === 0) {
    return; // Don't log when no one is connected
  }
  
  const now = Date.now();
  console.log(`\n📊 Connected Clients Report (${new Date().toISOString()})`);
  console.log(`   Total active clients: ${activeClientsCount}`);
  console.log(`   Master sessions: ${masterSessionsCount}`);
  
  if (activeClientsCount > 0) {
    console.log(`\n   📱 Active Clients:`);
    Object.keys(clientActivity).forEach(sessionId => {
      const startTime = clientStartTimes[sessionId] || clientActivity[sessionId];
      const elapsedMs = now - startTime;
      const elapsedMinutes = Math.floor(elapsedMs / 60000);
      const elapsedSeconds = Math.floor((elapsedMs % 60000) / 1000);
      
      // Check if this session is also a master
      const isMaster = Object.values(masterRoles).includes(sessionId);
      const masterInfo = isMaster ? ' 👑' : '';
      
      console.log(`      ${sessionId.substring(0, 8)}... - ${elapsedMinutes}m ${elapsedSeconds}s${masterInfo}`);
    });
  }
  
  if (masterSessionsCount > 0) {
    console.log(`\n   👑 Master Sessions:`);
    Object.keys(heartbeats).forEach(sessionId => {
      const startTime = sessionStartTimes[sessionId] || heartbeats[sessionId];
      const elapsedMs = now - startTime;
      const elapsedMinutes = Math.floor(elapsedMs / 60000);
      const elapsedSeconds = Math.floor((elapsedMs % 60000) / 1000);
      
      // Check which airports this session is master of
      const masterAirports = Object.keys(masterRoles).filter(icao => masterRoles[icao] === sessionId);
      const airportInfo = masterAirports.length > 0 ? ` (${masterAirports.join(', ')})` : '';
      
      console.log(`      ${sessionId.substring(0, 8)}... - ${elapsedMinutes}m ${elapsedSeconds}s${airportInfo}`);
    });
  }
  
  console.log('');
}

// Log connected clients every minute
setInterval(logConnectedClients, 60000);

// Weather endpoints
app.get('/api/v1/airports/:icao/weather', getDataHandler('weather'));
app.post('/api/v1/airports/:icao/weather', postDataHandler('weather'));

// Events endpoints (default to empty array)
app.get('/api/v1/airports/:icao/events', getDataHandler('events'));
app.post('/api/v1/airports/:icao/events', postDataHandler('events'));

// Runway modes endpoints
app.get('/api/v1/airports/:icao/runway-modes', getDataHandler('runwayModes'));
app.post('/api/v1/airports/:icao/runway-modes', postDataHandler('runwayModes'));

app.get('/api/v1/airports/:icao/master-role', (req, res) => {
  const icao = req.params.icao.toUpperCase();
  const sessionId = req.headers['x-session-uuid'];
  
  if (!isValidICAO(icao)) {
    return res.status(400).json({ error: 'Invalid ICAO code' });
  }
  
  // Update client activity if session ID is provided
  if (sessionId) {
    updateClientActivity(sessionId);
    
    // Update heartbeat only if this session is the current master
    const currentMaster = masterRoles[icao];
    if (currentMaster === sessionId) {
      updateHeartbeat(sessionId);
    }
  }
  
  const currentMaster = masterRoles[icao];
  const isMaster = currentMaster === sessionId;
  
  res.json({
    isMaster,
    currentMaster,
    sessionId
  });
});

app.post('/api/v1/airports/:icao/master-role', (req, res) => {
  const icao = req.params.icao.toUpperCase();
  const sessionId = req.headers['x-session-uuid'];
  
  if (!isValidICAO(icao)) {
    return res.status(400).json({ error: 'Invalid ICAO code' });
  }
  
  if (!sessionId) {
    return res.status(400).json({ error: 'x-session-uuid header is required' });
  }
  
  // Update client activity
  updateClientActivity(sessionId);
  
  const acquired = acquireMasterRole(icao, sessionId);

  if (acquired) {
    // Status code 200 for successful acquisition
    res.status(200);
  } else {
    // Status code 409 for conflict (already held by another session)
    res.status(409);
  }
  
  res.json({
    acquired,
    isMaster: acquired,
    sessionId,
    message: acquired ? 'Master role acquired' : 'Master role already held by another session'
  });
});

app.delete('/api/v1/airports/:icao/master-role', (req, res) => {
  const icao = req.params.icao.toUpperCase();
  const sessionId = req.headers['x-session-uuid'];
  
  if (!isValidICAO(icao)) {
    return res.status(400).json({ error: 'Invalid ICAO code' });
  }
  
  if (!sessionId) {
    return res.status(400).json({ error: 'x-session-uuid header is required' });
  }
  
  // Update client activity
  updateClientActivity(sessionId);
  
  const currentMaster = masterRoles[icao];
  
  if (!currentMaster) {
    return res.status(404).json({
      message: 'No master role exists for this airport',
      sessionId,
      icao
    });
  }
  
  if (currentMaster !== sessionId) {
    return res.status(403).json({
      message: 'You are not the master for this airport',
      sessionId,
      currentMaster,
      icao
    });
  }
  
  // Release the master role and clean up heartbeat
  delete masterRoles[icao];
  delete heartbeats[sessionId];
  delete sessionStartTimes[sessionId];
  
  res.json({
    message: 'Master role released successfully',
    sessionId,
    icao,
    released: true
  });
});

// Minimum spacing endpoints
app.get('/api/v1/airports/:icao/minimum-spacing', getDataHandler('minimumSpacing'));
app.post('/api/v1/airports/:icao/minimum-spacing', postDataHandler('minimumSpacing'));

// Get all data for a specific airport
app.get('/api/v1/airports/:icao', (req, res) => {
  const icao = req.params.icao.toUpperCase();
  const sessionId = req.headers['x-session-uuid'];
  
  if (!isValidICAO(icao)) {
    return res.status(400).json({ error: 'Invalid ICAO code' });
  }
  
  // Update client activity if session ID is provided
  if (sessionId) {
    updateClientActivity(sessionId);
  }
  
  const airport = airportData[icao] || {};
  res.json({
    icao,
    weather: airport.weather || null,
    events: airport.events || [],
    runwayModes: airport.runwayModes || null,
    minimumSpacing: airport.minimumSpacing || null
  });
});

// List all airports that have data
app.get('/api/v1/airports', (req, res) => {
  const airports = Object.keys(airportData).map(icao => ({
    icao,
    hasWeather: !!airportData[icao].weather,
    hasEvents: !!(airportData[icao].events && airportData[icao].events.length > 0),
    hasRunwayModes: !!airportData[icao].runwayModes,
    hasMinimumSpacing: !!airportData[icao].minimumSpacing
  }));
  
  res.json({ airports });
});

app.get('/api/v1/compat', async (req, res) => {
  const clientVer = req.headers['x-client-version'] || "0.0.0";

  // Fetch latest version dynamically
  const latestClientVersion = await getLatestVersionCached();
  const minClientVersion = "1.2.0"; // keep this as your minimum supported version

  if (!latestClientVersion) {
    return res.status(500).json({ error: 'Failed to fetch latest client version' });
  }

  const status =
    compareVersions(clientVer, minClientVersion) < 0
      ? "update-required"
      : compareVersions(clientVer, latestClientVersion) < 0
        ? "update-recommended"
        : "ok";

  res.json({
    apiVersion: "1.0.0",
    latestClientVersion,
    minClientVersion,
    status,
    message:
      status === "update-required"
        ? "Your client version is no longer supported. Please update."
        : status === "update-recommended"
          ? `A newer version (${latestClientVersion}) of the client is available.`
          : "Your client is up to date."
  });
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

app.get('/api/v1/latest-client-version', async (req, res) => {
  const latestVersion = await getLatestVersionCached();
  
  if (!latestVersion) {
    return res.status(500).json({ error: 'Failed to fetch latest client version' });
  }
  
  res.json({ latestClientVersion: latestVersion });
});

// Client activity endpoint (replaces dedicated heartbeat for non-masters)
app.post('/api/v1/heartbeat', (req, res) => {
  const sessionId = req.headers['x-session-uuid'];
  
  if (!sessionId) {
    return res.status(400).json({ error: 'x-session-uuid header is required' });
  }
  
  updateClientActivity(sessionId);
  
  res.json({
    message: 'Client activity updated',
    sessionId,
    timestamp: new Date().toISOString()
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Simple Airport API Server running on port ${PORT}`);
  console.log(`📖 API Documentation:`);
  console.log(`   GET  /api/v1/airports/:icao/weather`);
  console.log(`   POST /api/v1/airports/:icao/weather`);
  console.log(`   GET  /api/v1/airports/:icao/events`);
  console.log(`   POST /api/v1/airports/:icao/events`);
  console.log(`   GET  /api/v1/airports/:icao/runway-modes`);
  console.log(`   POST /api/v1/airports/:icao/runway-modes`);
  console.log(`   GET  /api/v1/airports/:icao/master-role`);
  console.log(`   POST /api/v1/airports/:icao/master-role`);
  console.log(`   DELETE /api/v1/airports/:icao/master-role`);
  console.log(`   GET  /api/v1/airports/:icao/minimum-spacing`);
  console.log(`   POST /api/v1/airports/:icao/minimum-spacing`);
  console.log(`   GET  /api/v1/airports/:icao`);
  console.log(`   GET  /api/v1/airports`);
  console.log(`   POST /api/v1/heartbeat`);
  console.log(`   GET  /health`);
  console.log(`📋 Headers: x-session-uuid optional (enables client tracking), required for master role operations`);
});
