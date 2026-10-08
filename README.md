# Personal Finance Management Platform with AI Smart Spending Planner

A university-level enterprise Java web application built on **Jakarta EE 10 / Servlet 6.0** with a deterministic, explainable **AI Smart Spending Planner**, pure **JDBC Data Access Object (DAO)** persistence layer, and background multithreading engine.

---

## 1. Architectural Constraints & Technology Stack

As per faculty specifications, this platform uses no heavyweight container frameworks (no Spring, Hibernate, or JPA).

| Layer | Component | Specification |
| :--- | :--- | :--- |
| **Language** | Java | Java 17 LTS (Records, Pattern Matching, Switch Expressions, Sealed Hierarchies) |
| **Web Container** | Apache Tomcat | Tomcat 10.1.x (Jakarta Servlet 6.0, JSP 3.1) |
| **Persistence** | Relational DB + JDBC | MySQL 8.0+ / JDBC 4.x (`PreparedStatement`, Transaction Demarcation) |
| **Templating** | Server-Side Views | JSP & JSTL 3.0 (`jakarta.servlet.jsp.jstl`), Bootstrap 5, Chart.js |
| **Security** | Auth & Integrity | jBCrypt 0.4, Anti-CSRF Tokens, RBAC Filters, Session Rotation |
| **Multithreading**| Background Workers | `ScheduledExecutorService` (2 worker threads), `@WebListener` |
| **Testing** | Automated Quality | JUnit 5 Jupiter, Mockito 5, H2 Database (in-memory test scope) |
| **Build Tool** | Apache Maven | Maven 3.9+ (`war` packaging) |

---

## 2. Prerequisites & Required Software

- **Java Development Kit (JDK)**: OpenJDK 17 LTS or higher (e.g. Eclipse Temurin 17)
- **Apache Maven**: 3.9.0 or higher
- **MySQL Database Server**: MySQL Community Server 8.0 or 8.4 LTS
- **Servlet Container**: Apache Tomcat 10.1.x (Note: Tomcat 9 is incompatible with `jakarta.*` packages)

---

## 3. Database Setup & Configuration

### A. Create and Seed Database

Open MySQL command line client or MySQL Workbench and execute the setup scripts:

```bash
# 1. Create schema and tables
mysql -u root -p < sql/schema.sql

# 2. Seed initial system categories, weights, and test accounts
mysql -u root -p < sql/seed.sql
```

The script initializes the `finance_db` database, creating 11 relational tables with foreign keys and indexes:
1. `users`
2. `categories`
3. `income`
4. `fixed_expenses`
5. `expenses`
6. `spending_plans`
7. `plan_items`
8. `budgets`
9. `notifications`
10. `system_settings`
11. `audit_logs`

### B. Configure Database Credentials

Configure database access using one of the following methods (in order of priority):

#### Option 1: Environment Variables (Recommended for Production & CI)
```bash
# Windows PowerShell:
$env:DB_URL = "jdbc:mysql://localhost:3306/finance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"
$env:DB_USER = "root"
$env:DB_PASSWORD = "your_mysql_password"

# Linux / macOS Bash:
export DB_URL="jdbc:mysql://localhost:3306/finance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"
export DB_USER="root"
export DB_PASSWORD="your_mysql_password"
```

#### Option 2: `src/main/resources/db.properties`
Copy `db.properties.example` to `src/main/resources/db.properties` and edit the credentials:
```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/finance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=your_mysql_password
```

---

## 4. Build, Test, and Deployment

### A. Run Automated Unit & Integration Tests

```bash
mvn test
```

All 38 tests run against the pure domain model, AI algorithms, Mockito mocks, and in-memory H2 MySQL compatibility layer.

### B. Package Web Application (WAR)

```bash
mvn clean package
```

The build compiles and produces `target/financeapp.war`.

### C. Run on Apache Tomcat 10.1

1. Copy `target/financeapp.war` into Tomcat's `webapps/` directory:
   ```bash
   cp target/financeapp.war $CATALINA_HOME/webapps/ROOT.war   # Or financeapp.war
   ```
2. Start Tomcat:
   ```bash
   # Windows:
   bin\startup.bat

   # Linux/macOS:
   bin/startup.sh
   ```
3. Navigate to `http://localhost:8080/financeapp` (or `http://localhost:8080` if deployed as `ROOT`).

---

## 5. Seeded Test Accounts

The seed dataset in `sql/seed.sql` creates accounts pre-hashed with BCrypt (cost factor 10). Credentials verified against `SecurityUtil.checkPassword`:

| Role | Email | Password | Description / Permissions |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@financeapp.com` | `AdminPassword123!` | Accesses `/admin/*`, manages users, adjusts AI planner weights and lockout settings. |
| **ADVISOR** | `advisor@financeapp.com` | `AdvisorPassword123!` | Accesses `/advisor/*`, reviews assigned client spending pace and financial health. |
| **USER** | `john@example.com` | `UserPassword123!` | Regular user with pre-seeded income ($5,800), fixed commitments ($2,000), and plan. |
| **USER** | `jane@example.com` | `UserPassword123!` | Fresh user account for onboarding and end-to-end plan creation. |

---

## 6. Review 1 Evaluation Mapping Table

This mapping table connects the academic evaluation criteria to the actual files in this repository:

| Evaluation Criterion | Implementation Details | Demonstrating Files |
| :--- | :--- | :--- |
| **1. Problem Understanding** | Formal requirements specification, relational schema with 11 tables, ER integrity, constraints, indexes, and seeded system parameters. | [`docs/spec.md`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/docs/spec.md)<br>[`sql/schema.sql`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/sql/schema.sql)<br>[`sql/seed.sql`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/sql/seed.sql)<br>[`pom.xml`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/pom.xml) |
| **2. Core Java: OOP & Inheritance** | Base abstract class `User` extended by `RegularUser`, `Advisor`, `Admin` implementing `abstract String getDashboardPath()`. Base `Transaction` extended by `Expense` (negative signed amount) and `Income` (positive signed amount). | [`com.financeapp.model.User`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/model/User.java)<br>[`com.financeapp.model.Transaction`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/model/Transaction.java)<br>[`com.financeapp.model.Expense`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/model/Expense.java)<br>[`com.financeapp.model.Income`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/model/Income.java) |
| **3. Core Java: Collections & Streams** | `LinkedHashMap` for deterministic category order. `Collectors.groupingBy()` on transaction streams for category totals and sorted `TreeMap<YearMonth, BigDecimal>` for monthly trend analytics. | [`com.financeapp.service.PlannerService`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/service/PlannerService.java)<br>[`com.financeapp.planner.DefaultStrategy`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/planner/DefaultStrategy.java) |
| **4. Core Java: Multithreading** | Application listener bootstrapping a 2-thread `ScheduledExecutorService`. Thread-safe counters (`AtomicInteger`), concurrent state tracking (`ConcurrentHashMap`), and lifecycle termination in `contextDestroyed`. | [`com.financeapp.listener.AppStartupListener`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/listener/AppStartupListener.java)<br>[`com.financeapp.task.PlanMonitorTask`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/task/PlanMonitorTask.java)<br>[`com.financeapp.task.RecurringEntryTask`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/task/RecurringEntryTask.java) |
| **5. Core Java: Custom Exceptions & Precision** | Custom exception hierarchy for business rule violations (`InsufficientFundsException`, `InvalidAmountException`, `DuplicateEmailException`, `UnauthorizedAccessException`, `DataAccessException`). Monetary accuracy via `BigDecimal` with `RoundingMode.HALF_UP`. | [`com.financeapp.exception`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/exception)<br>[`com.financeapp.planner.RecalculationResult`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/planner/RecalculationResult.java) |
| **6. JDBC: Persistence & DAOs** | Complete DAO pattern with JDBC interfaces and implementations. Connection pooling/factory via `DBConnection.java`, `try-with-resources` on all connections, statements, and result sets. | [`com.financeapp.dao`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/dao)<br>[`com.financeapp.dao.jdbc`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/dao/jdbc)<br>[`com.financeapp.util.DBConnection`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/util/DBConnection.java) |
| **7. JDBC: Security & Transactions** | 100% Parameterized queries with `PreparedStatement` to neutralize SQL injection. Single JDBC transaction boundary (`setAutoCommit(false)`, `commit()`, `rollback()`) across expense logging, recalculation, and notification. | [`com.financeapp.servlet.ExpenseServlet`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/servlet/ExpenseServlet.java)<br>[`com.financeapp.dao.jdbc.JdbcExpenseDAO`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/dao/jdbc/JdbcExpenseDAO.java) |
| **8. Servlets & Web Architecture** | Controllers handling HTTP `GET`/`POST`, PRG (Post-Redirect-Get) pattern, flash session messaging. Views strictly in `WEB-INF/views` using JSTL with zero scriptlet logic. Chart.js visualization. | [`com.financeapp.servlet.DashboardServlet`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/servlet/DashboardServlet.java)<br>[`com.financeapp.servlet.PlanSetupServlet`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/servlet/PlanSetupServlet.java)<br>[`WEB-INF/views/user/dashboard.jsp`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/webapp/WEB-INF/views/user/dashboard.jsp) |
| **9. Web Security & Filters** | Authentication filter for protected routes, Role-Based Access Control filter (`RoleFilter`) logging 403 violations to `audit_logs`, Anti-CSRF token verification filter (`CsrfFilter`), session fixation protection via `changeSessionId()`. | [`com.financeapp.filter.AuthFilter`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/filter/AuthFilter.java)<br>[`com.financeapp.filter.RoleFilter`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/filter/RoleFilter.java)<br>[`com.financeapp.filter.CsrfFilter`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/filter/CsrfFilter.java)<br>[`com.financeapp.servlet.LoginServlet`](file:///c:/working/PERSONAL%20FINENCE%20MANAGMENT/src/main/java/com/financeapp/servlet/LoginServlet.java) |

---

## 7. AI Smart Spending Planner Logic

The AI planner operates on an explainable, deterministic heuristic:
1. **Available Disposable Income**:
   $$\text{Available} = \sum \text{Income} - \sum \text{FixedObligations}$$
   If $\text{Available} \le 0$, throws `InsufficientFundsException` with financial consolidation guidance.
2. **Allocation Strategies**:
   - `DefaultStrategy`: Food (40%), Savings (20%), Emergency (15%), Entertainment (10%), Other (15%). Last category absorbs any rounding difference to ensure $\sum \text{Allocations} = \text{Available}$.
   - `SavingsFocusedStrategy`: Evaluates user's explicit goal, clamps savings share between 20% and 40%, scales other envelopes proportionally, and alerts the user if goal was clamped.
3. **Pace Recalculation**:
   - `PlanStatus`: `UNDER` (ratio $< 80\%$), `ON_TRACK` ($80\% - 110\%$), `OVER` ($> 110\%$).
   - $\text{Adjusted Daily Limit} = \frac{\max(\text{Budget} - \text{Spent}, 0)}{\text{DaysLeft}}$.
4. **Explainable Rules**:
   - `OverspendingRule`: Fires when pace ratio exceeds 110%, explaining budget trajectory and updated daily ceiling.
   - `SurplusRule`: Fires when pace ratio is below 80%, highlighting spending surplus.
   - `LowBalanceRule`: Fires when remaining balance is dangerously low relative to days remaining in billing cycle.
