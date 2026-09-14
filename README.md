# 🍽️ Restaurant Order Management System

A Java **Multithreading** project that simulates a restaurant where multiple customers place orders, chefs prepare them concurrently, and waiters deliver them.

## 📌 Scenario

* 👥 **10 Customers**
* 👨‍🍳 **3 Chefs**
* 🧑‍🍳 **2 Waiters**
* 📋 VIP orders are processed before NORMAL orders
* 📦 Thread-safe shared inventory

## ⚙️ How It Works

```text
Customers
    ↓
PriorityBlockingQueue
    ↓
3 Chefs
    ↓
Thread-Safe Inventory
    ↓
Completed Order Queue
    ↓
2 Waiters
    ↓
Customers
```

### Flow

1. Each customer runs in a separate thread and places an order.
2. Orders are stored in a `PriorityBlockingQueue`.
3. Three chef threads process orders concurrently.
4. VIP orders receive higher priority.
5. Inventory is synchronized to prevent race conditions.
6. Chefs prepare food using `Thread.sleep()` to simulate cooking time.
7. Completed orders are added to a `BlockingQueue`.
8. Two waiter threads deliver the orders.
9. Customers are notified when their order is delivered.
10. The restaurant shuts down gracefully after all orders are processed.

## 🧵 Concepts Demonstrated

* `Thread`
* `Runnable`
* `ExecutorService`
* Thread Pool
* `BlockingQueue`
* `PriorityBlockingQueue`
* `synchronized` / `ReentrantLock`
* `sleep()`
* `wait()` / `notify()`
* `join()`
* Graceful thread termination

## 🔐 Thread Safety

Inventory is a shared resource accessed by multiple chefs.

The **check-and-deduct operation** is synchronized so that two chefs cannot modify the inventory at the same time.

This prevents **race conditions** and incorrect inventory values.

## 💀 Deadlock Prevention

Deadlocks can occur when multiple threads wait for locks held by each other.

They are prevented by:

* Avoiding unnecessary nested locks
* Keeping synchronized sections small
* Acquiring multiple locks in a consistent order
* Using concurrent collections where possible

## ▶️ Run the Project

Requires **Java 8+**.

```bash
javac *.java
java Main
```

Or run `Main.java` directly from your IDE.

## 🎯 Objective

This project demonstrates how Java multithreading can be used to build a **concurrent, thread-safe producer-consumer system** similar to a real-world restaurant workflow.

---

**Author:** Chetanya Bansal
