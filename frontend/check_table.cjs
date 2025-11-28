const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const dbPath = path.resolve(__dirname, '../bank.db');
console.log('Checking DB at:', dbPath);

const db = new sqlite3.Database(dbPath);

db.serialize(() => {
  db.all("SELECT name FROM sqlite_master WHERE type='table' AND name='user_security_settings'", (err, rows) => {
    if (err) {
      console.error('Error checking table:', err);
    } else {
      console.log('Table user_security_settings exists:', rows.length > 0);
      db.all("PRAGMA table_info(user_security_settings)", (err, cols) => {
        if (err) console.error(err);
        else console.log('Columns:', cols);
      });
      db.all("SELECT name, sql FROM sqlite_master WHERE type='trigger'", (err, rows) => {
        if (err) console.error(err);
        else console.log('Triggers:', rows);
      });
    }
  });
});

db.close();
