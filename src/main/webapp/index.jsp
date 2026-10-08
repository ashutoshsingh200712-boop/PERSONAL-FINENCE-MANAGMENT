<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Personal Finance Management Platform | AI Smart Spending Planner</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-base: #0B0F19;
            --bg-card: rgba(22, 30, 49, 0.7);
            --bg-glass: rgba(255, 255, 255, 0.04);
            --border-glass: rgba(255, 255, 255, 0.08);
            --primary: #6366F1;
            --primary-gradient: linear-gradient(135deg, #6366F1 0%, #8B5CF6 50%, #EC4899 100%);
            --accent-cyan: #06B6D4;
            --accent-emerald: #10B981;
            --text-main: #F8FAFC;
            --text-muted: #94A3B8;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: var(--bg-base);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
            background-image: 
                radial-gradient(circle at 15% 20%, rgba(99, 102, 241, 0.15) 0%, transparent 40%),
                radial-gradient(circle at 85% 30%, rgba(236, 72, 153, 0.12) 0%, transparent 45%),
                radial-gradient(circle at 50% 80%, rgba(6, 182, 212, 0.1) 0%, transparent 50%);
        }

        header {
            padding: 1.5rem 3rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid var(--border-glass);
            backdrop-filter: blur(12px);
        }

        .logo {
            font-size: 1.25rem;
            font-weight: 800;
            letter-spacing: -0.02em;
            background: var(--primary-gradient);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .hero {
            max-width: 1000px;
            margin: 4rem auto;
            padding: 0 2rem;
            text-align: center;
            flex: 1;
        }

        .badge {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            padding: 0.4rem 1rem;
            border-radius: 9999px;
            background: rgba(99, 102, 241, 0.1);
            border: 1px solid rgba(99, 102, 241, 0.3);
            color: #A5B4FC;
            font-size: 0.85rem;
            font-weight: 600;
            margin-bottom: 1.5rem;
        }

        h1 {
            font-size: 3.5rem;
            font-weight: 800;
            line-height: 1.15;
            letter-spacing: -0.03em;
            margin-bottom: 1.5rem;
        }

        h1 span {
            background: var(--primary-gradient);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        p.lead {
            font-size: 1.2rem;
            color: var(--text-muted);
            line-height: 1.7;
            max-width: 720px;
            margin: 0 auto 3rem auto;
        }

        .grid-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 1.5rem;
            text-align: left;
            margin-top: 2rem;
        }

        .card {
            background: var(--bg-card);
            border: 1px solid var(--border-glass);
            border-radius: 16px;
            padding: 1.75rem;
            backdrop-filter: blur(16px);
            transition: transform 0.25s ease, border-color 0.25s ease;
        }

        .card:hover {
            transform: translateY(-4px);
            border-color: rgba(99, 102, 241, 0.4);
        }

        .card-icon {
            width: 44px;
            height: 44px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 1.25rem;
            font-size: 1.25rem;
        }

        .card h3 {
            font-size: 1.2rem;
            font-weight: 700;
            margin-bottom: 0.5rem;
        }

        .card p {
            color: var(--text-muted);
            font-size: 0.95rem;
            line-height: 1.5;
        }

        footer {
            padding: 2rem;
            text-align: center;
            color: var(--text-muted);
            font-size: 0.85rem;
            border-top: 1px solid var(--border-glass);
        }
    </style>
</head>
<body>
    <header>
        <div class="logo">
            <span>◆</span> FinApp AI
        </div>
        <div style="font-size: 0.9rem; color: var(--text-muted);">
            Jakarta EE 10 • Servlet 6.0 • MySQL 8
        </div>
    </header>

    <main class="hero">
        <div class="badge">
            <span>✨</span> Automated Smart Financial Planning Platform
        </div>
        <h1>Master Your Wealth with <span>AI-Driven Spending Plans</span></h1>
        <p class="lead">
            University Personal Finance Management Platform engineered with Jakarta Servlet 6.0, pure JDBC DAO architecture, and deterministic smart allocation heuristics.
        </p>

        <div class="grid-cards">
            <div class="card">
                <div class="card-icon" style="background: rgba(99, 102, 241, 0.15); color: #818CF8;">⚖️</div>
                <h3>Smart Allocations</h3>
                <p>AI heuristics calculate disposable income and distribute dynamic envelopes (Food 40%, Savings 20%, Emergency 15%, etc.).</p>
            </div>
            <div class="card">
                <div class="card-icon" style="background: rgba(16, 185, 129, 0.15); color: #34D399;">🛡️</div>
                <h3>Role-Based Security</h3>
                <p>Protected client-advisor-admin dashboards powered by jBCrypt password hashing and Jakarta EE filters.</p>
            </div>
            <div class="card">
                <div class="card-icon" style="background: rgba(6, 182, 212, 0.15); color: #22D3EE;">📊</div>
                <h3>Audit & Analytics</h3>
                <p>Full traceability with database audit logs, budget threshold warnings, and interactive spending metrics.</p>
            </div>
        </div>
    </main>

    <footer>
        Personal Finance Management Platform &copy; 2026. Built with Jakarta EE &amp; MySQL 8.
    </footer>
</body>
</html>
