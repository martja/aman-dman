// Test script to verify overwrite behavior
const PORT = 3000;
const BASE_URL = `http://localhost:${PORT}/api/v1/airports`;

async function testOverwrite() {
  console.log('🧪 Testing overwrite behavior...\n');
  
  try {
    // Test 1: Post initial events
    console.log('1. Posting initial events...');
    const response1 = await fetch(`${BASE_URL}/KJFK/events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify([
        { type: 'delay', flight: 'AA123' },
        { type: 'gate_change', flight: 'DL456' }
      ])
    });
    
    if (response1.ok) {
      console.log('✅ Initial events posted');
    } else {
      console.log('❌ Failed to post initial events');
      return;
    }
    
    // Test 2: Get the events to verify they were stored
    console.log('\n2. Getting events...');
    const getResponse1 = await fetch(`${BASE_URL}/KJFK/events`);
    const data1 = await getResponse1.json();
    console.log('Current events:', JSON.stringify(data1.data, null, 2));
    
    // Test 3: Post new events (should completely replace the old ones)
    console.log('\n3. Posting NEW events (should overwrite)...');
    const response2 = await fetch(`${BASE_URL}/KJFK/events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify([
        { type: 'cancellation', flight: 'UA789' }
      ])
    });
    
    if (response2.ok) {
      console.log('✅ New events posted');
    } else {
      console.log('❌ Failed to post new events');
      return;
    }
    
    // Test 4: Get events again to verify overwrite
    console.log('\n4. Getting events after overwrite...');
    const getResponse2 = await fetch(`${BASE_URL}/KJFK/events`);
    const data2 = await getResponse2.json();
    console.log('Events after overwrite:', JSON.stringify(data2.data, null, 2));
    
    // Verify overwrite worked
    if (data2.data.length === 1 && data2.data[0].flight === 'UA789') {
      console.log('\n✅ SUCCESS: Overwrite behavior is working correctly!');
      console.log('   - Old events (AA123, DL456) were completely replaced');
      console.log('   - Only new event (UA789) exists');
    } else {
      console.log('\n❌ FAILED: Data was appended instead of overwritten');
      console.log('   - Expected only 1 event (UA789)');
      console.log(`   - Got ${data2.data.length} events`);
    }
    
  } catch (error) {
    console.log('❌ Test failed:', error.message);
  }
}

// Run the test
testOverwrite();
