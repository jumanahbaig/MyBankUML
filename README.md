# 🏦 BankUml: Banking System Simulation

Welcome to **BankUml**, a Java-based banking application designed to simulate core banking operations such as account management, transactions, and receipts.  

This project demonstrates the use of Object-Oriented Programming (OOP) principles, including **Inheritance**, **Encapsulation**, **Abstraction**, and **Polymorphism**, strictly following the provided UML diagram.

## 📌 Features

- **Account Management**: Create and manage multiple types of bank accounts.
- **Transaction Handling**: Simulate payments and generate receipts.
- **UML-Driven Design**: Class structure directly follows the given UML diagram.

## 📊 Diagram

![BankUml Hierarchical Diagram](./BankUml_Hierarchical_Diagram.drawio.svg)

This diagram demonstrates the classes within the BankUml project template. Inheritance relationships are shown with solid line arrows, whilst implied relationships are shown with dashed line arrows. This is only a reference for the template, and you are free to change the application architecture as you see fit!

## 🚀 How to Run

Make sure you have the following installed:

- Java
- Maven (if Lombok is missing or not working correctly)

1. Clone the repository:

```bash
git clone https://github.com/jumanahbaig/MyBankUML.git
cd MyBankUML
```

2. Compile the code:

```bash
javac -cp "libs/*" bank/*.java 
```

3. Run the program:

```bash
# Linux/MAC
java -cp ".:libs/*" bank.Main
# Windows
java -cp ".;libs/*" bank.Main
```

To redownload the Lombok jar:

```bash
mvn dependency:copy-dependencies -DoutputDirectory=./libs
```

---

Originally developed by [@shayanaminaei](https://github.com/shayanaminaei)

Cloned from [M-PERSIC/BankUml](https://github.com/M-PERSIC/BankUml)

## Quick Start

**Prerequisites:**
- Java 21 or newer
- Maven
- Node.js (with npm)

1. Install dependencies:

```bash
cd frontend
npm install
```

2. Run the application:

**Windows:**
```bash
compile_run.bat
```

**Mac/Linux:**
```bash
# Terminal 1 - Start frontend
cd frontend && npm run dev

# Terminal 2 - Start backend (in a new terminal)
cd backend && mvn clean compile exec:java
```

This will start both the frontend and backend servers.
