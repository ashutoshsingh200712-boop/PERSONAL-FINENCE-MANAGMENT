# Personal Finance Management Platform with AI Smart Spending Planner
## Software Architecture & Specification Document

### 1. Project Overview & Scope
The Personal Finance Management Platform is a university-level Jakarta EE 10 enterprise web application designed to help individuals and financial advisors track, analyze, and optimize personal finances. It incorporates a deterministic rule-based and heuristic "AI Smart Spending Planner" that automatically calculates recommended budget distributions, savings goals, emergency fund allocations, and discretionary spending envelopes.

Strict Architectural Constraint (Faculty Requirement):
- **Plain Jakarta Servlet 6.0 & JSP 3.1** on **Apache Tomcat 10.1**
- **Plain JDBC** with the **Data Access Object (DAO)** pattern
- **No Spring, Hibernate, JPA, or heavyweight container frameworks**
- Core Java 17 LTS
- Modern CSS (responsive, glassmorphism, dashboard cards)
- MySQL 8.0+ Relational Database

---

### 2. Technology Stack & Environment
| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Language** | Java 17 LTS | Records, Pattern Matching, Switch Expressions |
| **Web Container** | Apache Tomcat 10.1.x | Jakarta Servlet 6.0, Jakarta Server Pages (JSP) 3.1 |
| **Templating & Tags** | JSP & JSTL 3.0 | `jakarta.servlet.jsp.jstl` tag libraries |
| **Database** | MySQL 8.0+ | InnoDB, UTF8MB4 charset, strict SQL mode |
| **Persistence** | JDBC 4.x + DAO Pattern | Parameterized `PreparedStatement`, transaction demarcation |
| **Security** | jBCrypt 0.4 + Jakarta Filters | Blowfish password hashing (cost 10+), RBAC Filters |
| **Testing** | JUnit 5 Jupiter + Mockito | Unit tests for DAOs, Services, and Planner Engine |
| **Build Tool** | Apache Maven 3.9+ | WAR packaging |

---

### 3. Core Roles & Permissions (RBAC)
1. **USER**:
   - Manages personal income streams, fixed recurring expenses, and daily expenses.
   - Generates monthly AI Smart Spending Plans based on income and obligations.
   - Sets category budget limits and receives threshold warning alerts (e.g. 80% consumed).
   - Views interactive charts, spending summaries, and exportable financial logs.
   - Receives guidance from an assigned financial advisor (if linked).
2. **ADVISOR**:
   - Accesses dashboard of linked client accounts (`advisor_id = users.id`).
   - Reviews client spending plans, budget deficits, and trends.
   - Sends financial advice notifications and spending recommendations to clients.
3. **ADMIN**:
   - Manages platform user accounts (activate, deactivate, reset lockout).
   - Configures global AI planner weights in `system_settings` (e.g. 40/20/15/10/15 default split).
   - Monitors audit logs (`audit_logs`) and system health diagnostics.

---

### 4. Domain Model & Database Schema

#### 4.1 Tables Summary
1. `users`: Authentication, credentials, role (USER/ADVISOR/ADMIN), account lockout fields, advisor relationship.
2. `categories`: Classification for spending and budgets (Food, Savings, Emergency, Entertainment, Other, etc.).
3. `income`: Individual recurring and one-time income streams.
4. `fixed_expenses`: Inflexible recurring obligations (rent, utilities, loan amortizations, subscriptions).
5. `expenses`: Discretionary and operational expenses tagged with dates and payment methods.
6. `spending_plans`: Monthly master planner records calculating disposable income per `(user_id, plan_month)`.
7. `plan_items`: Breakdown per category for each spending plan with weight percentages and allocations.
8. `budgets`: User-defined monthly category limits with customizable alert triggers (default: 80%).
9. `notifications`: In-app system alerts, over-budget warnings, and advisor recommendations.
10. `system_settings`: Platform-wide configurations and default AI planner category weights.
11. `audit_logs`: Traceability log for security events, administrative updates, and login anomalies.

#### 4.2 Monetary Accuracy
All monetary columns are strictly typed as `DECIMAL(12,2)` in MySQL and mapped to `java.math.BigDecimal` in Java with rounding mode `RoundingMode.HALF_UP` to prevent floating-point inaccuracies.

---

### 5. AI Smart Spending Planner Engine
The AI Smart Spending Planner implements a deterministic financial heuristic algorithm:
1. **Income Computation**: Aggregates all active monthly recurring income streams and verified one-time entries for target month $M$.
   $$\text{Total Income } (I) = \sum \text{Income}_i$$
2. **Fixed Obligations Deduction**: Computes mandatory non-negotiable living costs:
   $$\text{Fixed Expenses } (F) = \sum \text{FixedExpense}_j$$
3. **Disposable Income Calculation**:
   $$\text{Disposable Income } (D) = I - F$$
4. **Deficit Detection & Advice**:
   - If $D \le 0$, system flags a high-priority financial distress alert (`DEFICIT_RISK`), advising fixed expense consolidation and emergency assistance.
5. **Dynamic Weight Allocation**:
   - Defaults loaded from `system_settings` (Food: 40%, Savings: 20%, Emergency: 15%, Entertainment: 10%, Other: 15%).
   - Planner adjusts weights dynamically based on user debt ratios and past spending deviations over rolling 3-month averages.
6. **Plan Item Generation**:
   $$\text{Allocation}_c = D \times \left(\frac{\text{Weight}_c}{100}\right)$$
   Guarantees $\sum \text{Allocation}_c = D$ without rounding leakage.

---

### 6. Package Architecture
- `com.financeapp.model`: POJO domain entities (`User`, `Category`, `Income`, `Expense`, `SpendingPlan`, etc.)
- `com.financeapp.dao`: Interface definitions for data access
- `com.financeapp.dao.jdbc`: Pure JDBC implementations with `PreparedStatement` and connection lifecycle
- `com.financeapp.planner`: AI Spending Planner algorithms, heuristics, and weight calculators
- `com.financeapp.service`: Business service layer orchestrating transactions, validations, and hashing
- `com.financeapp.servlet`: Controller servlets handling HTTP requests, forwarding to JSPs
- `com.financeapp.filter`: Authentication filter, RBAC authorization filter, UTF-8 encoding filter
- `com.financeapp.listener`: Context initialization, DB health checks, scheduler lifecycle
- `com.financeapp.task`: Scheduled background jobs (e.g., automated monthly plan generation, threshold checks)
- `com.financeapp.exception`: Custom application, database, and validation exceptions
- `com.financeapp.util`: `DBConnection`, `PasswordUtil`, `DateUtil`, `ValidationUtil`
