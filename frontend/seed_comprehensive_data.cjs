const API_URL = 'http://localhost:8080/api';

// Realistic fake user data with correct UPPERCASE roles
const users = [
    // Default users for easy login
    { username: 'customer', password: 'password123', role: 'CUSTOMER', firstName: 'Customer', lastName: 'User' },
    { username: 'teller', password: 'password123', role: 'TELLER', firstName: 'Teller', lastName: 'User' },
    { username: 'admin', password: 'password123', role: 'ADMIN', firstName: 'Admin', lastName: 'User' },
    
    // Customer users with realistic names
    { username: 'jdoe', password: 'password123', role: 'CUSTOMER', firstName: 'John', lastName: 'Doe' },
    { username: 'ssmith', password: 'password123', role: 'CUSTOMER', firstName: 'Sarah', lastName: 'Smith' },
    { username: 'mjohnson', password: 'password123', role: 'CUSTOMER', firstName: 'Michael', lastName: 'Johnson' },
    { username: 'ebrown', password: 'password123', role: 'CUSTOMER', firstName: 'Emily', lastName: 'Brown' },
    { username: 'dwilliams', password: 'password123', role: 'CUSTOMER', firstName: 'David', lastName: 'Williams' },
    { username: 'jmiller', password: 'password123', role: 'CUSTOMER', firstName: 'Jennifer', lastName: 'Miller' },
    { username: 'rdavis', password: 'password123', role: 'CUSTOMER', firstName: 'Robert', lastName: 'Davis' },
    { username: 'lgarcia', password: 'password123', role: 'CUSTOMER', firstName: 'Lisa', lastName: 'Garcia' },
    { username: 'wmartinez', password: 'password123', role: 'CUSTOMER', firstName: 'William', lastName: 'Martinez' },
    { username: 'aanderson', password: 'password123', role: 'CUSTOMER', firstName: 'Amanda', lastName: 'Anderson' },
    { username: 'jthomas', password: 'password123', role: 'CUSTOMER', firstName: 'James', lastName: 'Thomas' },
    { username: 'mwilson', password: 'password123', role: 'CUSTOMER', firstName: 'Mary', lastName: 'Wilson' },
    { username: 'cmoore', password: 'password123', role: 'CUSTOMER', firstName: 'Christopher', lastName: 'Moore' },
    { username: 'ptaylor', password: 'password123', role: 'CUSTOMER', firstName: 'Patricia', lastName: 'Taylor' },
    
    // Additional tellers
    { username: 'teller2', password: 'password123', role: 'TELLER', firstName: 'Alice', lastName: 'Cooper' },
    { username: 'teller3', password: 'password123', role: 'TELLER', firstName: 'Brian', lastName: 'Foster' },
];

// Transaction descriptions by type
const transactionDescriptions = {
    credit: [
        'Direct Deposit - Salary',
        'Paycheck',
        'Cash Deposit',
        'Transfer from Savings',
        'Tax Refund',
        'Bonus Payment',
        'Investment Return',
        'Freelance Income',
        'Gift Deposit',
        'Reimbursement'
    ],
    debit: [
        'ATM Withdrawal',
        'Cash Withdrawal',
        'Transfer to Savings',
        'Bill Payment',
        'Rent Payment',
        'Mortgage Payment',
        'Utility Payment',
        'Insurance Premium',
        'Student Loan Payment',
        'Medical Expense',
        'Amazon Purchase',
        'Grocery Store - Walmart',
        'Gas Station - Shell',
        'Restaurant - Chipotle',
        'Online Shopping',
        'Starbucks Coffee',
        'Target Shopping',
        'Netflix Subscription',
        'Spotify Premium',
        'Gym Membership',
        'Phone Bill',
        'Internet Service',
        'Car Payment',
        'Credit Card Payment',
        'Insurance Payment'
    ]
};

// Helper function to get random item from array
function randomItem(array) {
    return array[Math.floor(Math.random() * array.length)];
}

// Helper function to generate random amount
function randomAmount(min, max) {
    return Math.round((Math.random() * (max - min) + min) * 100) / 100;
}

// Helper function to generate account number
function generateAccountNumber() {
    return 'ACC-' + Math.floor(Math.random() * 1000000).toString().padStart(6, '0');
}

// Helper function to create account via direct database insert
async function createAccountForUser(userId, accountType) {
    const sqlite3 = require('sqlite3').verbose();
    const db = new sqlite3.Database('../bank.db');

    const accountNumber = generateAccountNumber();
    const initialBalance = randomAmount(500, 10000);

    // Map friendly names to DB values
    const dbAccountType = accountType === 'checking' ? 'CHECK' : 
                          accountType === 'savings' ? 'SAVING' : 
                          accountType === 'card' ? 'CARD' : 'CHECK';

    return new Promise((resolve, reject) => {
        const sql = `INSERT INTO accounts (customer_id, account_type, account_number, balance, created_at) 
                     VALUES (?, ?, ?, ?, datetime('now'))`;
        
        db.run(sql, [userId, dbAccountType, accountNumber, initialBalance], function(err) {
            db.close();
            if (err) {
                console.error(`    ❌ Failed to create ${accountType} account: ${err.message}`);
                reject(err);
            } else {
                console.log(`    ✅ Created ${accountType} account ${accountNumber} with balance $${initialBalance.toFixed(2)} (ID: ${this.lastID})`);
                resolve({ id: this.lastID, accountNumber, balance: initialBalance });
            }
        });
    });
}

// Helper function to generate realistic transactions
function generateTransactions(accountType, count = 15) {
    const transactions = [];
    
    // Start with initial deposit (credit)
    transactions.push({
        amount: randomAmount(1000, 5000),
        type: 'credit',
        description: 'Initial Deposit'
    });

    // Generate random transactions
    for (let i = 0; i < count; i++) {
        // 70% chance of debit (spending), 30% chance of credit (income)
        const type = Math.random() > 0.3 ? 'debit' : 'credit';
        let amount;

        if (type === 'credit') {
            amount = randomAmount(100, 3000);
        } else {
            amount = randomAmount(10, 500);
        }

        transactions.push({
            amount: amount,
            type: type,
            description: randomItem(transactionDescriptions[type])
        });
    }

    // For savings accounts, mostly credits
    if (accountType === 'savings') {
        return transactions.filter(t => t.type === 'credit' || Math.random() > 0.8);
    }

    return transactions;
}

// Main seeding function
async function seed() {
    console.log('\n🌱 Starting comprehensive data seeding...\n');
    console.log('=' .repeat(60));

    let totalAccounts = 0;
    let totalTransactions = 0;

    for (const user of users) {
        try {
            console.log(`\n👤 Processing user: ${user.firstName} ${user.lastName} (@${user.username})`);
            
            // 1. Create User
            const createRes = await fetch(`${API_URL}/users`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    firstName: user.firstName,
                    lastName: user.lastName,
                    username: user.username,
                    password: user.password,
                    role: user.role
                })
            });

            if (createRes.status === 201) {
                console.log(`  ✅ Created ${user.role} user`);
            } else if (createRes.status === 409) {
                console.log(`  ℹ️  User already exists`);
            } else {
                const error = await createRes.text();
                console.error(`  ❌ Failed to create user: ${createRes.status} - ${error}`);
                continue;
            }

            // 2. Login to get user ID and token
            const loginRes = await fetch(`${API_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: user.username,
                    password: user.password
                })
            });

            if (!loginRes.ok) {
                console.error(`  ❌ Failed to login`);
                continue;
            }

            const loginData = await loginRes.json();
            const userId = loginData.user.id;
            const token = loginData.token;
            console.log(`  🔑 Logged in (ID: ${userId})`);

            // 3. Create accounts and transactions (only for customers)
            if (user.role === 'CUSTOMER') {
                console.log(`  💳 Creating accounts...`);
                
                // Determine which accounts to create (randomly for variety)
                const accountTypes = ['checking', 'savings'];
                const shouldCreateCredit = Math.random() > 0.5; // 50% chance
                if (shouldCreateCredit) {
                    accountTypes.push('card');
                }

                for (const accountType of accountTypes) {
                    try {
                        // Create account
                        const account = await createAccountForUser(userId, accountType);
                        totalAccounts++;

                        // Generate and add transactions
                        const transactionCount = accountType === 'savings' ? 8 : 15;
                        const transactions = generateTransactions(accountType, transactionCount);
                        
                        console.log(`    📊 Adding ${transactions.length} transactions...`);

                        let successCount = 0;
                        for (const txn of transactions) {
                            try {
                                // Note: The API endpoint expects 'deposit'/'withdrawal'/'payment'
                                // but we want to insert 'credit'/'debit' into the DB.
                                // Since we are using the API here, we should use the API types.
                                // Wait, the API converts them!
                                // API: deposit -> credit, withdrawal -> debit.
                                // So we should send 'deposit'/'withdrawal' to the API.
                                
                                // Let's map back for the API call
                                const apiType = txn.type === 'credit' ? 'deposit' : 'withdrawal';
                                
                                const txnRes = await fetch(`${API_URL}/accounts/${account.id}/transactions`, {
                                    method: 'POST',
                                    headers: { 
                                        'Content-Type': 'application/json',
                                        'Authorization': `Bearer ${token}`
                                    },
                                    body: JSON.stringify({
                                        amount: txn.amount,
                                        type: apiType,
                                        description: txn.description
                                    })
                                });

                                if (txnRes.ok) {
                                    successCount++;
                                    totalTransactions++;
                                }
                            } catch (error) {
                                // Silent fail for individual transactions
                            }
                        }
                        console.log(`    ✅ Added ${successCount}/${transactions.length} transactions`);

                        // Small delay to avoid overwhelming the server
                        await new Promise(resolve => setTimeout(resolve, 100));

                    } catch (error) {
                        console.error(`    ❌ Error creating ${accountType} account: ${error.message}`);
                    }
                }
            } else {
                console.log(`  ℹ️  Skipping accounts (${user.role} role)`);
            }

        } catch (error) {
            console.error(`❌ Error processing ${user.username}:`, error.message);
        }
    }

    console.log('\n' + '='.repeat(60));
    console.log('🎉 Seeding completed!');
    console.log(`📊 Summary:`);
    console.log(`   - Users: ${users.length}`);
    console.log(`   - Accounts: ${totalAccounts}`);
    console.log(`   - Transactions: ${totalTransactions}`);
    console.log('='.repeat(60) + '\n');
}

// Run the seed function
seed()
    .then(() => process.exit(0))
    .catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
