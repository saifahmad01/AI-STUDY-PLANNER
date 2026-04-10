import React from 'react';
import './App.css';
import mascot from './assets/mascot.png';

function App() {
  return (
    <div className="app-wrapper">
      <div className="app-container">
        {/* ── Navbar ─────────────────────────────────── */}
        <nav className="navbar" id="navbar">
          <div className="nav-logo" id="nav-logo">
            <span className="logo-icon">💡</span>
            <span className="logo-text">AI Study</span>
          </div>
          <div className="nav-links">
            <a href="#dashboard" className="nav-link" id="nav-dashboard">•Dashboard•</a>
            <a href="#about" className="nav-link" id="nav-about">•About•</a>
            <button className="btn-signup" id="btn-signup">Sign Up</button>
          </div>
        </nav>

        {/* ── Hero Section ───────────────────────────── */}
        <section className="hero" id="hero-section">
          <div className="hero-bg-shapes">
            <div className="shape shape-1"></div>
            <div className="shape shape-2"></div>
            <div className="shape shape-3"></div>
            <div className="shape shape-dot shape-dot-1">💛</div>
            <div className="shape shape-dot shape-dot-2">💛</div>
          </div>

          <div className="hero-content">
            <div className="hero-text">
              <h1 className="hero-title">
                AI Smart <span className="text-highlight">Study</span>
                <br />Planner
              </h1>
              <p className="hero-subtitle">Boost your study efficiency with AI!</p>
              <button className="btn-get-started" id="btn-get-started">
                Get Started
              </button>
            </div>
            <div className="hero-image">
              <img src={mascot} alt="AI Study Planner Robot Mascot" className="mascot-img" />
            </div>
          </div>
        </section>

        {/* ── Features Section ───────────────────────── */}
        <section className="features" id="features-section">
          <FeatureCard
            icon="📋"
            iconBg="feature-icon-blue"
            title="Personalized Study Plans"
            description="Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy"
          />
          <FeatureCard
            icon="🔔"
            iconBg="feature-icon-orange"
            title="Task Reminders"
            description="Lorem ipsum dolor sit amet, csectetuer adipiscing elit, sed diam nonummy"
          />
          <FeatureCard
            icon="📊"
            iconBg="feature-icon-teal"
            title="Progress Tracking"
            description="Lorem ipsum dolor sit amet, ossectetuer adipiscing elit, sed diam nonummy"
          />
        </section>
      </div>
    </div>
  );
}

function FeatureCard({ icon, iconBg, title, description }) {
  return (
    <div className="feature-card">
      <div className={`feature-icon ${iconBg}`}>
        <span className="feature-emoji">{icon}</span>
      </div>
      <h3 className="feature-title">{title}</h3>
      <p className="feature-desc">{description}</p>
    </div>
  );
}

export default App;
