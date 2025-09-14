const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());

// In-memory storage for airport data
const airportData = {};

// Helper function to validate ICAO codes (4 letters)
function isValidICAO(icao) {
  return /^[A-Z]{4}$/.test(icao.toUpperCase());
}

// Generic handler for GET endpoints
function getDataHandler(dataType, defaultValue = null) {
  return (req, res) => {
    const icao = req.params.icao.toUpperCase();
    
    if (!isValidICAO(icao)) {
      return res.status(400).json({ error: 'Invalid ICAO code' });
    }
    
    const data = airportData[icao]?.[dataType] || defaultValue;
    res.json(data);
  };
}

// Generic handler for POST endpoints
function postDataHandler(dataType) {
  return (req, res) => {
    const icao = req.params.icao.toUpperCase();
    
    if (!isValidICAO(icao)) {
      return res.status(400).json({ error: 'Invalid ICAO code' });
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

// Weather endpoints
app.get('/api/v1/airports/:icao/weather', getDataHandler('weather'));
app.post('/api/v1/airports/:icao/weather', postDataHandler('weather'));

// Events endpoints (default to empty array)
app.get('/api/v1/airports/:icao/events', getDataHandler('events'));
app.post('/api/v1/airports/:icao/events', postDataHandler('events'));

// Runway modes endpoints
app.get('/api/v1/airports/:icao/runway-modes', getDataHandler('runwayModes'));
app.post('/api/v1/airports/:icao/runway-modes', postDataHandler('runwayModes'));

// Minimum spacing endpoints
app.get('/api/v1/airports/:icao/minimum-spacing', getDataHandler('minimumSpacing'));
app.post('/api/v1/airports/:icao/minimum-spacing', postDataHandler('minimumSpacing'));

// Get all data for a specific airport
app.get('/api/v1/airports/:icao', (req, res) => {
  const icao = req.params.icao.toUpperCase();
  
  if (!isValidICAO(icao)) {
    return res.status(400).json({ error: 'Invalid ICAO code' });
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

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
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
  console.log(`   GET  /api/v1/airports/:icao/minimum-spacing`);
  console.log(`   POST /api/v1/airports/:icao/minimum-spacing`);
  console.log(`   GET  /api/v1/airports/:icao`);
  console.log(`   GET  /api/v1/airports`);
  console.log(`   GET  /health`);
});
