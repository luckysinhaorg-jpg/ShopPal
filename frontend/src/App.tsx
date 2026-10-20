import { Fragment, useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type FormEvent } from 'react'
import { AlertTriangle, ArrowDownToLine, ArrowUpRight, BarChart3, Boxes, Check, ChevronDown, CircleHelp, FileUp, LoaderCircle, LogIn, LogOut, Mic, PackageCheck, Search, ShieldCheck, Sparkles, Store, Truck, UserCheck, X } from 'lucide-react'
import './style.css'

type Profile = 'KIRANA' | 'FOOTWEAR' | 'CLOTHING' | 'ELECTRONICS' | 'PHARMACY'
type User = { id: number; username: string; ownerName: string; shopName: string; businessProfile: Profile }
type Dashboard = { profile: string; productCount: number; sellableUnits: number; unitsSoldLast30Days: number; estimatedStockExposure: number; expiryRiskBatches: number; stockRiskProducts: number; productsToReorder: number; salesDataSource: string; limitations: string; shopName?: string; ownerName?: string }
type Recommendation = { productId: number; sku: string; productName: string; proposedOrderQuantity: number; method: string; targetDemand: number; currentSellableStock: number; coverageDays: number; forecastDemand: number; riskLevel: string; riskExplanation: string; reason: string; syntheticData: boolean; assumptions: string }
type Product = { id: number; sku: string; name: string; profile: Profile; stock: number; sellingPrice: number; purchaseCost: number | null }
type Expiry = { productId: number; productName: string; batchCode: string; quantity: number; expiryDate: string | null; daysRemaining: number | null; level: string; suggestedAction: string }
type ReturnAnalysis = { productId: number; sku: string; productName: string; unitsSold: number; unitsReturned: number; returnRate: number; reasons: string[]; grossSales: number | null; grossSalesComplete: boolean; estimatedHoldingCost: number | null; estimatedProfit: number | null; profitPerThousandInvested: number | null; formulaAndLimitations: string }
type Durability = { productId: number; productName: string; material: string | null; leatherGrade: string | null; supplier: string | null; score: number; observations: number; durabilityRelatedReturns: number; evidenceStrength: string; explanation: string }
type Preview = { rows: { sku: string; date: string; unitsSold: number; unitPrice: number }[]; errors: string[] }
type View = 'overview' | 'expiry' | 'returns' | 'suppliers' | 'assistant'
type ChatMessage = {
  role: 'assistant' | 'user'
  text: string
  spokenText?: string
  source?: string
  mcpTool?: string
  isSupported?: boolean
  recommendations?: Recommendation[]
  expiryAlerts?: Expiry[]
  inventorySummary?: { profile: string; productCount: number; sellableUnits: number; estimatedStockExposure: number; expiryRiskBatches: number; limitations: string } | null
  disclaimer?: string
  sampleNotice?: string
}

type AlexaResponse = {
  query: string
  intent: string
  spokenText: string
  detailedAnswer: string
  mcpToolUsed: string
  isSupported: boolean
  accountInfo: { shopName: string; ownerName: string; businessProfile: string }
  recommendations: Recommendation[]
  expiryAlerts: Expiry[]
  inventorySummary: { profile: string; productCount: number; sellableUnits: number; estimatedStockExposure: number; expiryRiskBatches: number; limitations: string } | null
  disclaimer: string
  sampleDataNotice: string
}

const API = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080/api'
const profiles: { id: Profile; label: string }[] = [
  { id: 'KIRANA', label: 'Kirana (Grocery)' },
  { id: 'PHARMACY', label: 'Pharmacy (Healthcare)' },
  { id: 'FOOTWEAR', label: 'Footwear & Shoes' },
  { id: 'CLOTHING', label: 'Clothing & Apparel' },
  { id: 'ELECTRONICS', label: 'Consumer Electronics' },
]

const inr = (value: number | null | undefined) => value == null ? '—' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value)
const pct = (value: number) => `${(value * 100).toFixed(1)}%`

function getInitials(name: string) {
  if (!name) return 'SP'
  const parts = name.trim().split(/\s+/)
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

export default function App() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('shoppal_token'))
  const [user, setUser] = useState<User | null>(() => {
    try {
      const saved = localStorage.getItem('shoppal_user')
      return saved ? JSON.parse(saved) : null
    } catch { return null }
  })
  const [showAuthModal, setShowAuthModal] = useState(!localStorage.getItem('shoppal_token'))
  const [authTab, setAuthTab] = useState<'login' | 'signup'>('login')
  const [authUsername, setAuthUsername] = useState('')
  const [authPassword, setAuthPassword] = useState('')
  const [authOwnerName, setAuthOwnerName] = useState('')
  const [authShopName, setAuthShopName] = useState('')
  const [authProfile, setAuthProfile] = useState<Profile>('KIRANA')
  const [authError, setAuthError] = useState('')
  const [authBusy, setAuthBusy] = useState(false)

  const profile: Profile = user ? user.businessProfile : 'KIRANA'
  const [view, setView] = useState<View>('overview')
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [recommendations, setRecommendations] = useState<Recommendation[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [expiry, setExpiry] = useState<Expiry[]>([])
  const [returns, setReturns] = useState<ReturnAnalysis[]>([])
  const [durability, setDurability] = useState<Durability[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [riskFilter, setRiskFilter] = useState('All items')
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<Preview | null>(null)
  const [importMessage, setImportMessage] = useState('')
  const [importBusy, setImportBusy] = useState(false)
  const [chat, setChat] = useState<ChatMessage[]>([{ role: 'assistant', text: 'Namaste! I can check stock, expiry and returns using the shop data currently loaded.' }])
  const [question, setQuestion] = useState('')
  const [chatBusy, setChatBusy] = useState(false)
  const fileInput = useRef<HTMLInputElement>(null)

  const api = useCallback(async <T,>(path: string, init?: RequestInit, explicitToken?: string | null): Promise<T> => {
    const headers = new Headers(init?.headers)
    const effectiveToken = explicitToken !== undefined ? explicitToken : token
    if (effectiveToken) {
      headers.set('Authorization', `Bearer ${effectiveToken}`)
    }
    const response = await fetch(`${API}${path}`, { ...init, headers })
    if (!response.ok) {
      let message = `${response.status} ${response.statusText}`
      try { const body = await response.json(); message = body.message || body.errors?.join(', ') || message } catch { /* keep HTTP message */ }
      throw new Error(message)
    }
    return response.json() as Promise<T>
  }, [token])

  // Check auth on startup
  useEffect(() => {
    if (token) {
      api<{ user: User }>('/auth/me', undefined, token)
        .then(res => {
          setUser(res.user)
          localStorage.setItem('shoppal_user', JSON.stringify(res.user))
        })
        .catch(() => {
          setToken(null)
          setUser(null)
          localStorage.removeItem('shoppal_token')
          localStorage.removeItem('shoppal_user')
          setShowAuthModal(true)
        })
    }
  }, [token, api])

  const refresh = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const profileQuery = `?profile=${profile}`
      const fetchList: Promise<any>[] = [
        api<Dashboard>(`/dashboard${profileQuery}`),
        api<Recommendation[]>(`/recommendations${profileQuery}`),
        api<Product[]>(`/products${profileQuery}`),
        api<Expiry[]>('/expiry?days=30'),
        api<ReturnAnalysis[]>(`/returns${profileQuery}`),
      ]
      if (profile === 'FOOTWEAR') {
        fetchList.push(api<Durability[]>('/durability'))
      } else {
        fetchList.push(Promise.resolve([]))
      }

      const [d, r, p, e, ret, dur] = await Promise.all(fetchList)
      setDashboard(d); setRecommendations(r); setProducts(p); setExpiry(e); setReturns(ret); setDurability(dur)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load the shop data.')
    } finally {
      setLoading(false)
    }
  }, [profile, api])

  useEffect(() => {
    if (!showAuthModal) {
      void refresh()
    }
  }, [refresh, showAuthModal])

  async function handleLogin(u?: string, p?: string) {
    const un = (u ?? authUsername).trim()
    const pw = p ?? authPassword
    if (!un || !pw) {
      setAuthError('Please enter username and password.')
      return
    }
    setAuthBusy(true); setAuthError('')
    try {
      const res = await api<{ token: string; user: User }>('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: un, password: pw })
      }, null)
      setToken(res.token)
      setUser(res.user)
      localStorage.setItem('shoppal_token', res.token)
      localStorage.setItem('shoppal_user', JSON.stringify(res.user))
      setShowAuthModal(false)
      setAuthPassword('')
      setView('overview')
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Login failed.')
    } finally {
      setAuthBusy(false)
    }
  }

  async function handleSignup(e: FormEvent) {
    e.preventDefault()
    if (!authUsername.trim() || !authPassword || !authOwnerName.trim() || !authShopName.trim()) {
      setAuthError('All fields are required.')
      return
    }
    setAuthBusy(true); setAuthError('')
    try {
      const res = await api<{ token: string; user: User }>('/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: authUsername.trim(),
          password: authPassword,
          ownerName: authOwnerName.trim(),
          shopName: authShopName.trim(),
          businessProfile: authProfile
        })
      }, null)
      setToken(res.token)
      setUser(res.user)
      localStorage.setItem('shoppal_token', res.token)
      localStorage.setItem('shoppal_user', JSON.stringify(res.user))
      setShowAuthModal(false)
      setAuthPassword('')
      setView('overview')
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Signup failed.')
    } finally {
      setAuthBusy(false)
    }
  }

  function handleLogout() {
    if (token) {
      void fetch(`${API}/auth/logout`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      })
    }
    setToken(null)
    setUser(null)
    localStorage.removeItem('shoppal_token')
    localStorage.removeItem('shoppal_user')
    setShowAuthModal(true)
  }

  const filteredProducts = useMemo(() => products.filter(p => {
    const matchesText = `${p.name} ${p.sku}`.toLowerCase().includes(query.toLowerCase())
    const rec = recommendations.find(item => item.productId === p.id)
    const matchesRisk = riskFilter === 'All items' || (riskFilter === 'Reorder' && (rec?.proposedOrderQuantity ?? 0) > 0)
      || (riskFilter === 'Stockout risk' && rec?.riskLevel === 'STOCKOUT_RISK')
      || (riskFilter === 'Overstock risk' && rec?.riskLevel === 'OVERSTOCK_RISK')
      || (riskFilter === 'Expiry' && expiry.some(x => x.productId === p.id && x.level !== 'OK'))
      || (riskFilter === 'Returns' && (returns.find(x => x.productId === p.id)?.returnRate ?? 0) > 0.05)
    return matchesText && matchesRisk
  }), [products, query, riskFilter, recommendations, returns, expiry])

  async function loadPreview(selected: File | null) {
    setFile(selected); setPreview(null); setImportMessage('')
    if (!selected) return
    setImportBusy(true)
    try {
      const body = new FormData(); body.append('file', selected)
      const result = await api<Preview>('/import/preview', { method: 'POST', body })
      setPreview(result)
      if (!result.errors.length) setImportMessage(`${result.rows.length} valid sales rows ready to import.`)
    } catch (err) { setImportMessage(err instanceof Error ? err.message : 'Preview failed.') }
    finally { setImportBusy(false) }
  }

  async function importFile() {
    if (!file || !preview || preview.errors.length) return
    setImportBusy(true); setImportMessage('')
    try {
      const body = new FormData(); body.append('file', file)
      const result = await api<{ importedRows: number; duplicate: boolean }>('/import', { method: 'POST', body })
      setImportMessage(result.duplicate ? 'This exact file was already imported.' : `${result.importedRows} sales rows imported.`)
      setPreview(null); setFile(null); if (fileInput.current) fileInput.current.value = ''
      await refresh()
    } catch (err) { setImportMessage(err instanceof Error ? err.message : 'Import failed.') }
    finally { setImportBusy(false) }
  }

  useEffect(() => {
    if (user) {
      const welcome = user.businessProfile === 'PHARMACY'
        ? `Namaste ${user.ownerName}! I'm your simulated Alexa+ voice assistant for ${user.shopName}. Try asking: "Which medicine batches are nearing expiry?" or "What stock needs restocking and why?"`
        : `Namaste ${user.ownerName}! I'm your simulated Alexa+ voice assistant for ${user.shopName}. Try asking: "What stock needs restocking and why?" or "Which batches are nearing expiry?"`
      setChat([
        {
          role: 'assistant',
          text: welcome,
          spokenText: welcome,
          source: 'Simulated Alexa+ Assistant (Connected to ShopPal MCP)',
          sampleNotice: `Sample Seed Data (${user.businessProfile})`,
          disclaimer: 'Simulated Alexa+ experience for Amazon Hackathon (Alexa+ track). Grounded on isolated shop records.'
        }
      ])
    }
  }, [user])

  async function ask(raw: string) {
    const text = raw.trim(); if (!text || chatBusy) return
    setQuestion(''); setChat(c => [...c, { role: 'user', text }]); setChatBusy(true)
    try {
      const res = await api<AlexaResponse>('/alexa/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: text })
      })
      setChat(c => [...c, {
        role: 'assistant',
        text: res.detailedAnswer,
        spokenText: res.spokenText,
        mcpTool: res.mcpToolUsed,
        isSupported: res.isSupported,
        recommendations: res.recommendations,
        expiryAlerts: res.expiryAlerts,
        inventorySummary: res.inventorySummary,
        disclaimer: res.disclaimer,
        sampleNotice: res.sampleDataNotice,
        source: `MCP Tool: ${res.mcpToolUsed}`
      }])
    } catch (err) {
      setChat(c => [...c, {
        role: 'assistant',
        text: err instanceof Error ? err.message : 'I could not retrieve that data.',
        source: 'request failed',
        isSupported: false
      }])
    } finally {
      setChatBusy(false)
    }
  }

  function submitQuestion(event: FormEvent) { event.preventDefault(); void ask(question) }

  // Navigation items: exclude footwear suppliers tab unless footwear profile
  const navItems: [View, string, any][] = [
    ['overview', 'Overview', BarChart3],
    ['expiry', profile === 'PHARMACY' ? 'Medicine Expiry' : 'Expiry Guard', ShieldCheck],
    ['returns', 'Returns', ArrowUpRight],
    ...(profile === 'FOOTWEAR' ? [['suppliers', 'Supplier insights', Truck] as [View, string, any]] : []),
    ['assistant', 'Simulated Alexa+', Mic],
  ]

  const currentShopName = user?.shopName || dashboard?.shopName || (profile === 'PHARMACY' ? 'Verma Medicos & Healthcare' : 'Sharma General Store')
  const currentOwnerName = user?.ownerName || dashboard?.ownerName || (profile === 'PHARMACY' ? 'Dr. Ramesh Verma' : 'Amit Sharma')

  return (
    <div className="app-shell">
      {/* Login & Signup Modal */}
      {showAuthModal && (
        <div className="auth-overlay">
          <div className="auth-card">
            <div className="auth-header">
              <div className="brand-mark"><Store size={22} /></div>
              <h2>ShopPal Account</h2>
              <p>Sign in to manage your shop’s inventory and stock predictions.</p>
              <div className="auth-tabs">
                <button className={`auth-tab ${authTab === 'login' ? 'active' : ''}`} onClick={() => { setAuthTab('login'); setAuthError('') }}>Sign In</button>
                <button className={`auth-tab ${authTab === 'signup' ? 'active' : ''}`} onClick={() => { setAuthTab('signup'); setAuthError('') }}>Create New Account</button>
              </div>
            </div>

            {authError && <div className="error-banner" style={{ margin: '14px 24px 0' }}><AlertTriangle size={15} /><span>{authError}</span></div>}

            {authTab === 'login' ? (
              <form className="auth-form" onSubmit={(e) => { e.preventDefault(); void handleLogin() }}>
                <div className="auth-field">
                  <label>EMAIL OR USERNAME</label>
                  <input type="text" placeholder="e.g. kirana@shoppal.local" value={authUsername} onChange={e => setAuthUsername(e.target.value)} required />
                </div>
                <div className="auth-field">
                  <label>PASSWORD</label>
                  <input type="password" placeholder="••••••••" value={authPassword} onChange={e => setAuthPassword(e.target.value)} required />
                </div>
                <button className="button button-green auth-submit" type="submit" disabled={authBusy}>
                  {authBusy ? <LoaderCircle className="spin" size={16} /> : <LogIn size={16} />}
                  <span>Sign In</span>
                </button>

                <div className="auth-demo-section">
                  <div className="auth-demo-title">OR SIGN IN AS REGISTERED DEMO OWNER:</div>
                  <div className="auth-demo-chips">
                    <button type="button" className="demo-chip" onClick={() => void handleLogin('kirana@shoppal.local', 'Kirana@123')}>
                      <Store size={13} /> Kirana Store
                    </button>
                    <button type="button" className="demo-chip" onClick={() => void handleLogin('pharmacy@shoppal.local', 'Pharmacy@123')}>
                      <ShieldCheck size={13} /> Pharmacy Shop
                    </button>
                    <button type="button" className="demo-chip" onClick={() => void handleLogin('footwear@shoppal.local', 'Footwear@123')}>
                      <Truck size={13} /> Footwear Shop
                    </button>
                  </div>
                </div>
              </form>
            ) : (
              <form className="auth-form" onSubmit={handleSignup}>
                <div className="auth-field">
                  <label>OWNER NAME</label>
                  <input type="text" placeholder="e.g. Ramesh Patel" value={authOwnerName} onChange={e => setAuthOwnerName(e.target.value)} required />
                </div>
                <div className="auth-field">
                  <label>SHOP NAME</label>
                  <input type="text" placeholder="e.g. Patel Superstore" value={authShopName} onChange={e => setAuthShopName(e.target.value)} required />
                </div>
                <div className="auth-field">
                  <label>EMAIL / USERNAME</label>
                  <input type="text" placeholder="e.g. patel@store.com" value={authUsername} onChange={e => setAuthUsername(e.target.value)} required />
                </div>
                <div className="auth-field">
                  <label>PASSWORD</label>
                  <input type="password" placeholder="Minimum 6 characters" value={authPassword} onChange={e => setAuthPassword(e.target.value)} required />
                </div>
                <div className="auth-field">
                  <label>SHOP TYPE (EXTENSIBLE TO OTHER BUSINESSES)</label>
                  <select value={authProfile} onChange={e => setAuthProfile(e.target.value as Profile)}>
                    {profiles.map(p => <option key={p.id} value={p.id}>{p.label}</option>)}
                  </select>
                </div>
                <button className="button button-green auth-submit" type="submit" disabled={authBusy}>
                  {authBusy ? <LoaderCircle className="spin" size={16} /> : <UserCheck size={16} />}
                  <span>Register & Open Dashboard</span>
                </button>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Sidebar */}
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><Store size={20} /></div>
          <div><strong>ShopPal</strong><span>Dukaan Dost</span></div>
        </div>

        <div className="shop-switch" onClick={() => setShowAuthModal(true)} title="Click to switch or log in account" style={{ cursor: 'pointer' }}>
          <div className="shop-avatar">{currentShopName.charAt(0).toUpperCase()}</div>
          <div>
            <small>{profile} ACCOUNT</small>
            <strong>{currentShopName}</strong>
            <span className="shop-tag">{profile}</span>
          </div>
          <ChevronDown size={14} />
        </div>

        <span className="nav-label">WORKSPACE</span>
        <nav>
          {navItems.map(([id, label, Icon]) => (
            <button key={id} className={`nav-item ${view === id ? 'active' : ''}`} onClick={() => setView(id)}>
              <Icon size={17} />
              <span>{label}</span>
              {id === 'expiry' && dashboard?.expiryRiskBatches ? <i>{dashboard.expiryRiskBatches}</i> : null}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="rating-shield">
            <div className="shield-icon"><ShieldCheck size={18} /></div>
            <div>
              <strong>Rating Shield</strong>
              <p>Catch stock risks before they become customer complaints.</p>
            </div>
          </div>
          <div className="sidebar-foot">
            Isolated Account Data <span className="green-dot" /> Live
          </div>
        </div>
      </aside>

      {/* Main Area */}
      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb">Workspace <span>/</span> {viewLabel(view, profile)}</div>
          <div className="top-actions">
            <span className={`connected ${error ? 'offline' : ''}`}><i /> {error ? 'API offline' : 'API connected'}</span>
            <button className="icon-button" aria-label="Help" onClick={() => setView('assistant')}><CircleHelp size={18} /></button>
            {user ? (
              <div className="user-menu">
                <div className="user-pill" title={`Logged in as ${user.username}`}>
                  <div className="user-avatar">{getInitials(currentOwnerName)}</div>
                  <strong>{currentOwnerName}</strong>
                  <span>{profile}</span>
                </div>
                <button className="logout-btn" onClick={handleLogout} title="Log out">
                  <LogOut size={13} />
                  <span>Log out</span>
                </button>
              </div>
            ) : (
              <button className="login-trigger-btn" onClick={() => setShowAuthModal(true)}>
                <LogIn size={14} />
                <span>Sign In</span>
              </button>
            )}
          </div>
        </header>

        <div className="page-content">
          <section className="page-heading">
            <div>
              <div className="eyebrow"><span className="eyebrow-dot" /> {profile} INVENTORY INTELLIGENCE</div>
              <h1>{heading(view, profile)}</h1>
              <p>{subtitle(view, profile)}</p>
            </div>
            <div className="heading-actions">
              <button className="button button-dark" onClick={() => fileInput.current?.click()}>
                <FileUp size={16} /> Import sales
              </button>
              <input ref={fileInput} type="file" accept=".csv,text/csv" hidden onChange={e => void loadPreview(e.target.files?.[0] ?? null)} />
            </div>
          </section>

          {error && (
            <div className="error-banner">
              <AlertTriangle size={17} />
              <div><strong>Could not load live shop data</strong><span>{error}. Check API connection and retry.</span></div>
              <button onClick={() => void refresh()} className="text-button">Retry</button>
            </div>
          )}

          {file && (
            <section className="import-panel">
              <div className="import-heading">
                <div><FileUp size={17} /><strong>{file.name}</strong><span>{(file.size / 1024).toFixed(1)} KB</span></div>
                <button className="icon-button" onClick={() => { setFile(null); setPreview(null); setImportMessage(''); if (fileInput.current) fileInput.current.value = '' }} aria-label="Close import"><X size={17} /></button>
              </div>
              {importBusy ? (
                <div className="inline-status"><LoaderCircle className="spin" size={16} /> Validating CSV…</div>
              ) : preview && (
                <>
                  <div className={`import-status ${preview.errors.length ? 'bad' : 'good'}`}>
                    {preview.errors.length ? <AlertTriangle size={15} /> : <Check size={15} />}
                    {preview.errors.length ? `${preview.errors.length} issue(s) found` : `${preview.rows.length} valid row(s)`}
                  </div>
                  {preview.errors.length > 0 && <ul className="csv-errors">{preview.errors.slice(0, 8).map((x, i) => <li key={i}>{x}</li>)}</ul>}
                  {preview.rows.length > 0 && (
                    <div className="preview-table">
                      <span>SKU</span><span>Date</span><span>Units</span><span>Price</span>
                      {preview.rows.slice(0, 4).map((r, i) => (
                        <Fragment key={i}>
                          <span>{r.sku}</span><span>{r.date}</span><span>{r.unitsSold}</span><span>{inr(r.unitPrice)}</span>
                        </Fragment>
                      ))}
                    </div>
                  )}
                  <div className="import-footer">
                    <span>{importMessage}</span>
                    <button className="button button-green" disabled={preview.errors.length > 0 || !preview.rows.length} onClick={() => void importFile()}><Check size={15} /> Import valid file</button>
                  </div>
                </>
              )}
            </section>
          )}

          {importMessage && !file && <div className="toast"><Check size={15} />{importMessage}</div>}

          {view === 'overview' && (
            <>
              {!loading && dashboard?.productCount === 0 && (
                <div className="error-banner">
                  <AlertTriangle size={17} />
                  <div>
                    <strong>No catalogue items found for this account</strong>
                    <span>Use &quot;Import sales&quot; to upload your {profile.toLowerCase()} products and sales history.</span>
                  </div>
                </div>
              )}
              <div className="data-notice">
                <Sparkles size={15} />
                <span>
                  {dashboard?.salesDataSource === 'SYNTHETIC_DEMO' ? 'Illustrative demo data · ' : dashboard?.salesDataSource === 'IMPORTED_SALES' ? 'Imported sales data · ' : dashboard?.salesDataSource === 'MIXED' ? 'Mixed demo and imported sales · ' : ''}
                  Forecasts are rule-based estimates, not guarantees.
                </span>
                <button onClick={() => {
                  const a = document.createElement('a')
                  a.href = `${API}/import/sample.csv?profile=${profile}`
                  a.download = `shoppal-${profile.toLowerCase()}-sample.csv`
                  a.click()
                }}>
                  <ArrowDownToLine size={14} /> Get {profile.toLowerCase()} CSV template
                </button>
              </div>

              <div className="metrics-grid">
                {[
                  { label: 'Units sold · 30 days', value: loading ? '—' : dashboard?.unitsSoldLast30Days ?? 0, sub: 'Recorded sales activity', icon: BarChart3, color: 'blue' },
                  { label: 'Sellable stock', value: loading ? '—' : dashboard?.sellableUnits ?? 0, sub: `Across ${dashboard?.productCount ?? 0} products`, icon: Boxes, color: 'green' },
                  { label: 'Stock risk', value: loading ? '—' : dashboard?.stockRiskProducts ?? 0, sub: 'Stockout or overstock signals', icon: AlertTriangle, color: 'orange' },
                  { label: 'Expiry risks', value: loading ? '—' : dashboard?.expiryRiskBatches ?? 0, sub: profile === 'PHARMACY' ? 'Medicines expired or due soon' : 'Expired or due in 30 days', icon: ShieldCheck, color: 'orange' },
                  { label: 'Stock exposure', value: loading ? '—' : inr(dashboard?.estimatedStockExposure), sub: 'Recorded cost × sellable units', icon: PackageCheck, color: 'purple' },
                ].map(card => (
                  <article className="metric-card" key={card.label}>
                    <div className={`metric-icon ${card.color}`}><card.icon size={18} /></div>
                    <div className="metric-label">{card.label}</div>
                    <div className="metric-value">{card.value}</div>
                    <div className="metric-sub">{card.sub}</div>
                  </article>
                ))}
              </div>

              <div className="content-grid">
                <section className="panel recommendations-panel">
                  <div className="panel-heading">
                    <div>
                      <div className="section-kicker">DECISION SUPPORT</div>
                      <h2>Recommended orders</h2>
                      <p>What to replenish, with the reason behind it.</p>
                    </div>
                    <button className="link-button" onClick={() => setRiskFilter('Reorder')}>View all <ArrowUpRight size={14} /></button>
                  </div>
                  {loading ? <Loading /> : recommendations.filter(r => r.proposedOrderQuantity > 0).slice(0, 4).map(r => (
                    <div className="recommendation" key={r.productId}>
                      <div className="product-avatar">{r.productName.slice(0, 1)}</div>
                      <div className="rec-main">
                        <strong>{r.productName}</strong>
                        <span>{r.sku} · {r.currentSellableStock} sellable now</span>
                        <p>{r.reason}</p>
                        <small>{r.assumptions}</small>
                      </div>
                      <div className="rec-order">
                        <b>{r.proposedOrderQuantity}</b>
                        <span>units</span>
                      </div>
                    </div>
                  ))}
                  {!loading && recommendations.every(r => !r.proposedOrderQuantity) && <Empty text="No order is suggested for this planning window." />}
                </section>

                <section className="panel expiry-panel">
                  <div className="panel-heading">
                    <div>
                      <div className="section-kicker">{profile === 'PHARMACY' ? 'PHARMA FRESHNESS' : 'QUALITY & FRESHNESS'}</div>
                      <h2>{profile === 'PHARMACY' ? 'Medicine Expiry Guard' : 'Expiry Guard'}</h2>
                      <p>{profile === 'PHARMACY' ? 'Monitor medicine batches and disposal dates.' : 'Keep batches moving in the right order.'}</p>
                    </div>
                    <button className="icon-button" onClick={() => setView('expiry')} aria-label="Open expiry alerts"><ArrowUpRight size={17} /></button>
                  </div>
                  {loading ? <Loading /> : expiry.filter(e => e.level !== 'OK').slice(0, 4).map(e => (
                    <div className="expiry-row" key={e.batchCode}>
                      <span className={`risk-dot ${e.level === 'EXPIRED' ? 'red' : e.level.includes('7') ? 'orange' : 'yellow'}`} />
                      <div>
                        <strong>{e.productName}</strong>
                        <span>{e.batchCode}</span>
                      </div>
                      <b className={e.level === 'EXPIRED' ? 'danger-text' : 'warning-text'}>
                        {e.level === 'EXPIRED' ? 'Expired' : e.daysRemaining == null ? 'Unknown' : `${e.daysRemaining}d left`}
                      </b>
                    </div>
                  ))}
                  {!loading && !expiry.some(e => e.level !== 'OK') && <Empty text="No batches need attention in the next 30 days." />}
                </section>
              </div>

              <section className="panel inventory-panel">
                <div className="panel-heading">
                  <div>
                    <div className="section-kicker">YOUR CATALOGUE ({profile})</div>
                    <h2>Inventory</h2>
                  </div>
                  <div className="inventory-tools">
                    <label className="search-box">
                      <Search size={15} />
                      <input placeholder="Search products…" value={query} onChange={e => setQuery(e.target.value)} />
                    </label>
                    <select value={riskFilter} onChange={e => setRiskFilter(e.target.value)} aria-label="Filter inventory">
                      <option>All items</option>
                      <option>Reorder</option>
                      <option>Stockout risk</option>
                      <option>Overstock risk</option>
                      <option>Expiry</option>
                      <option>Returns</option>
                    </select>
                  </div>
                </div>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr><th>PRODUCT</th><th>SKU</th><th>SELLABLE STOCK</th><th>SELL PRICE</th><th>ORDER SUGGESTION</th><th>STATUS</th></tr>
                    </thead>
                    <tbody>
                      {loading ? (
                        <tr><td colSpan={6}><Loading /></td></tr>
                      ) : filteredProducts.map(p => {
                        const r = recommendations.find(x => x.productId === p.id)
                        const expiryRisk = expiry.some(e => e.productId === p.id && e.level !== 'OK')
                        const riskClass = expiryRisk ? 'attention' : r?.riskLevel === 'STOCKOUT_RISK' ? 'stockout' : r?.riskLevel === 'OVERSTOCK_RISK' ? 'overstock' : (r?.proposedOrderQuantity ?? 0) > 0 ? 'reorder' : 'healthy'
                        const riskLabel = expiryRisk ? 'Expiry risk' : r?.riskLevel === 'STOCKOUT_RISK' ? 'Stockout risk' : r?.riskLevel === 'OVERSTOCK_RISK' ? 'Overstock' : (r?.proposedOrderQuantity ?? 0) > 0 ? 'Reorder' : r?.riskLevel === 'UNKNOWN' ? 'Insufficient history' : 'Balanced'
                        return (
                          <tr key={p.id}>
                            <td><div className="table-product"><span>{p.name.slice(0, 1)}</span><strong>{p.name}</strong></div></td>
                            <td className="muted-cell">{p.sku}</td>
                            <td>{p.stock}<small className="cell-unit"> units</small></td>
                            <td>{inr(p.sellingPrice)}</td>
                            <td>{r?.proposedOrderQuantity ? `${r.proposedOrderQuantity} units` : '—'}</td>
                            <td><span className={`status-pill ${riskClass}`}>{riskLabel}</span></td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                  {!loading && !filteredProducts.length && <Empty text="No products match this search." />}
                </div>
              </section>

              <div className="bottom-note">
                <ShieldCheck size={16} />
                <span>Rating Shield helps you spot avoidable risks; it does not guarantee customer ratings or demand.</span>
                <span className="note-count">{dashboard?.productsToReorder ?? 0} products may need attention</span>
              </div>
            </>
          )}

          {view === 'expiry' && (
            <section className="panel full-panel">
              <PanelTitle
                kicker={profile === 'PHARMACY' ? 'PHARMACY EXPIRY & BATCH TRACKING' : `${profile} EXPIRY GUARD`}
                title={profile === 'PHARMACY' ? 'Medicine batch freshness' : 'Batch freshness'}
                description={profile === 'PHARMACY' ? 'Expired medicine stock must never be dispensed. Unknown dates require physical verification.' : 'Expired stock is never counted as sellable. Unknown dates are called out for a manual check.'}
              />
              {loading ? <Loading /> : expiry.length ? expiry.map(x => (
                <div className="detail-row" key={x.batchCode}>
                  <span className={`risk-dot ${x.level === 'EXPIRED' ? 'red' : x.level.includes('7') ? 'orange' : 'yellow'}`} />
                  <div className="detail-grow">
                    <strong>{x.productName} <small>{x.batchCode}</small></strong>
                    <span>{x.quantity} units · {x.expiryDate ?? 'Expiry date unknown'}</span>
                    <p>{x.suggestedAction}</p>
                  </div>
                  <span className={`status-pill ${x.level === 'EXPIRED' ? 'attention' : 'reorder'}`}>{x.level.replaceAll('_', ' ')}</span>
                </div>
              )) : <Empty text="No expiry alert batches found." />}
            </section>
          )}

          {view === 'returns' && (
            <section className="panel full-panel">
              <PanelTitle kicker="CUSTOMER EXPERIENCE" title="Returns & quality signals" description="Rates use returned units ÷ units sold. Financial results are estimates only when costs and prices are present." />
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr><th>PRODUCT</th><th>UNITS SOLD</th><th>RETURNED</th><th>RETURN RATE</th><th>GROSS SALES</th><th>EST. PROFIT</th><th>REASONS</th></tr>
                  </thead>
                  <tbody>
                    {loading ? <tr><td colSpan={7}><Loading /></td></tr> : returns.map(r => (
                      <tr key={r.productId}>
                        <td><strong>{r.productName}</strong><small className="block-muted">{r.sku}</small></td>
                        <td>{r.unitsSold}</td>
                        <td>{r.unitsReturned}</td>
                        <td>{pct(r.returnRate)}</td>
                        <td>{r.grossSalesComplete ? inr(r.grossSales) : 'Unavailable'}</td>
                        <td>{r.estimatedProfit == null ? 'Unavailable' : inr(r.estimatedProfit)}</td>
                        <td>{r.reasons.join(', ') || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <p className="fine-print">Profit formula: sales revenue − estimated refunds at current selling price − purchase cost of non-returned units − holding cost when supplied. Missing data stays unavailable.</p>
            </section>
          )}

          {view === 'suppliers' && profile === 'FOOTWEAR' && (
            <section className="panel full-panel">
              <PanelTitle kicker="FOOTWEAR PROFILE" title="Supplier durability signals" description="Scores use recorded durability-related return reasons. Small samples show low evidence strength." />
              {loading ? <Loading /> : durability.map(d => (
                <div className="durability-card" key={d.productId}>
                  <div className="score-ring" style={{ '--score': `${d.score}%` } as CSSProperties}>
                    <span>{d.score}</span>
                  </div>
                  <div className="detail-grow">
                    <strong>{d.productName}</strong>
                    <span>{d.supplier ?? 'Supplier not recorded'} · {d.material ?? 'Material not recorded'}{d.leatherGrade ? ` · ${d.leatherGrade}` : ''}</span>
                    <p>{d.observations} observation(s) · {d.durabilityRelatedReturns} durability-related return(s) · <b className="evidence">{d.evidenceStrength} evidence</b></p>
                    <small>{d.explanation}</small>
                  </div>
                </div>
              ))}
            </section>
          )}

          {view === 'assistant' && (
            <section className="assistant-layout">
              <div className="panel chat-panel">
                <div className="chat-title">
                  <div className="alexa-avatar alexa-glow"><Mic size={18} /></div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <strong>Simulated Alexa+ Assistant</strong>
                      <span className="alexa-pill-tag">ALEXA+ TRACK</span>
                    </div>
                    <span>
                      <i className="green-dot" /> Connected to {currentShopName} ({profile}) · Real MCP Server
                    </span>
                  </div>
                </div>

                <div className="chat-messages">
                  {chat.map((m, i) => (
                    <div key={i} className={`chat-line ${m.role}`}>
                      <div className={`chat-bubble ${m.role === 'assistant' ? 'alexa-bubble' : ''}`}>
                        {m.role === 'assistant' && m.spokenText && (
                          <div className="alexa-spoken">
                            <small style={{ display: 'block', fontSize: '9px', color: '#1f5f34', fontWeight: 700, marginBottom: '4px' }}>
                              🎙️ SIMULATED ALEXA+ SPOKEN ANSWER:
                            </small>
                            "{m.spokenText}"
                          </div>
                        )}

                        {m.role === 'assistant' && m.mcpTool && m.mcpTool !== 'none' && (
                          <div style={{ display: 'flex', gap: '6px', alignItems: 'center', flexWrap: 'wrap', margin: '4px 0 8px' }}>
                            <span className="alexa-tool-badge">🔧 Invoked MCP Tool: {m.mcpTool}</span>
                            {m.sampleNotice && <span className="alexa-sample-badge">{m.sampleNotice}</span>}
                          </div>
                        )}

                        {m.text.split('\n').map((line, n) => {
                          if (line.startsWith('### ')) {
                            return <h4 key={n} style={{ margin: '8px 0 4px', fontSize: '11px', color: '#1f2920' }}>{line.replace('### ', '')}</h4>
                          }
                          return <span key={n}>{line}</span>
                        })}

                        {/* Render rich recommendations cards if available */}
                        {m.recommendations && m.recommendations.length > 0 && (
                          <div className="alexa-cards-container">
                            {m.recommendations.map(r => (
                              <div className="alexa-rec-card" key={r.productId}>
                                <div className="alexa-rec-top">
                                  <div>
                                    <div className="alexa-rec-title">{r.productName}</div>
                                    <div className="alexa-rec-sku">{r.sku}</div>
                                  </div>
                                  <span className="alexa-rec-order">Order +{r.proposedOrderQuantity}</span>
                                </div>
                                <div className="alexa-rec-metrics">
                                  <span>Stock: <b>{r.currentSellableStock}</b></span>
                                  <span>Forecast: <b>{r.forecastDemand.toFixed(1)}</b></span>
                                  <span>Coverage: <b>{r.coverageDays}d</b></span>
                                </div>
                                <div className="alexa-rec-reason">{r.reason}</div>
                              </div>
                            ))}
                          </div>
                        )}

                        {/* Render rich expiry alert cards if available */}
                        {m.expiryAlerts && m.expiryAlerts.length > 0 && (
                          <div className="alexa-cards-container">
                            {m.expiryAlerts.map((e, idx) => (
                              <div className={`alexa-expiry-card ${e.level === 'EXPIRED' ? 'expired' : 'upcoming'}`} key={idx}>
                                <div className="alexa-expiry-header">
                                  <strong>{e.productName}</strong>
                                  <span className={`alexa-expiry-badge ${e.level === 'EXPIRED' ? 'danger' : e.level.includes('7') || e.level.includes('15') ? 'warning' : 'info'}`}>
                                    {e.level.replaceAll('_', ' ')}
                                  </span>
                                </div>
                                <div style={{ fontSize: '9px', color: '#687369' }}>
                                  Batch: <code>{e.batchCode}</code> · {e.quantity} units · {e.expiryDate ? (e.daysRemaining != null && e.daysRemaining < 0 ? `Expired ${Math.abs(e.daysRemaining)}d ago` : `Due in ${e.daysRemaining}d`) : 'No date'}
                                </div>
                                <div className="alexa-action-text">{e.suggestedAction}</div>
                              </div>
                            ))}
                          </div>
                        )}

                        {/* Render rich summary card if available */}
                        {m.inventorySummary && (
                          <div className="alexa-cards-container">
                            <div className="alexa-rec-card" style={{ gridColumn: '1 / -1' }}>
                              <div className="alexa-rec-top">
                                <div className="alexa-rec-title">{currentShopName} Inventory Snapshot</div>
                                <span className="status-pill healthy">{m.inventorySummary.profile}</span>
                              </div>
                              <div className="alexa-rec-metrics" style={{ marginTop: '8px' }}>
                                <span>Catalog: <b>{m.inventorySummary.productCount} products</b></span>
                                <span>Sellable: <b>{m.inventorySummary.sellableUnits} units</b></span>
                                <span>Exposure: <b>{inr(m.inventorySummary.estimatedStockExposure)}</b></span>
                                <span>Expiry Risks: <b>{m.inventorySummary.expiryRiskBatches} batches</b></span>
                              </div>
                            </div>
                          </div>
                        )}

                        {m.disclaimer && (
                          <div className="alexa-footer-meta">
                            <span>🛡️ {m.disclaimer}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                  {chatBusy && (
                    <div className="chat-line assistant">
                      <div className="chat-bubble alexa-bubble">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#005a9c', fontSize: '11px' }}>
                          <LoaderCircle className="spin" size={16} />
                          <span>Alexa+ routing query to ShopPal MCP tools & isolated database…</span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Workflow suggestion chips */}
                <div style={{ padding: '0 16px 10px' }}>
                  <div style={{ fontSize: '9px', fontWeight: 700, color: '#889387', marginBottom: '6px', letterSpacing: '0.5px' }}>
                    DEMONSTRATION WORKFLOWS (ALEXA+ TRACK):
                  </div>
                  <div className="alexa-workflow-chips">
                    <button
                      type="button"
                      className="alexa-chip workflow-1"
                      onClick={() => void ask('What stock needs restocking and why?')}
                    >
                      🛒 Workflow 1 (Kirana): "What stock needs restocking and why?"
                    </button>
                    <button
                      type="button"
                      className="alexa-chip workflow-2"
                      onClick={() => void ask('Which medicine batches are nearing expiry?')}
                    >
                      💊 Workflow 2 (Pharmacy): "Which medicine batches are nearing expiry?"
                    </button>
                    <button
                      type="button"
                      className="alexa-chip"
                      onClick={() => void ask('Give me an inventory summary')}
                    >
                      📊 "Give me an inventory summary"
                    </button>
                    <button
                      type="button"
                      className="alexa-chip unsupported"
                      onClick={() => void ask('What is the weather in Delhi today?')}
                    >
                      ☁️ Unsupported Test: "What is the weather?"
                    </button>
                  </div>
                </div>

                <form className="chat-input" onSubmit={submitQuestion}>
                  <input
                    value={question}
                    onChange={e => setQuestion(e.target.value)}
                    placeholder="Ask simulated Alexa+ (e.g. 'What stock needs restocking and why?')..."
                  />
                  <button disabled={!question.trim() || chatBusy} aria-label="Send question">
                    <ArrowUpRight size={17} />
                  </button>
                </form>
                <p className="chat-disclaimer">
                  Simulated Alexa+ experience for Amazon Hackathon. Not an actual connection to Amazon Alexa+ gated toolkit.
                </p>
              </div>

              <aside className="panel assistant-aside">
                <div className="section-kicker">ALEXA+ SIMULATOR</div>
                <h3>Amazon Developer Hackathon</h3>
                <p>
                  Official Hackathon FAQ confirms participants cannot access the gated Alexa+ MCP Toolkit or CLI;
                  the simulated Alexa+ experience is an eligible and recommended alternative.
                </p>

                <div className="provenance-note" style={{ flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 700, color: '#2b5a35' }}>
                    <ShieldCheck size={16} /> Strict Account Isolation
                  </div>
                  <span>
                    Logged in as <b>{currentOwnerName}</b> ({currentShopName}). The assistant operates strictly on this shop's catalog and batch records.
                  </span>
                </div>

                <div className="provenance-note" style={{ flexDirection: 'column', gap: '6px', marginTop: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 700, color: '#0c5460' }}>
                    <Boxes size={16} /> MCP Specification 2025-11-25
                  </div>
                  <span>
                    Supported over Streamable HTTP at <code>POST /mcp</code> with tools:
                    <code>get_recommendations</code>, <code>get_expiry_alerts</code>, <code>get_inventory_summary</code>, <code>analyze_returns</code>.
                  </span>
                </div>
              </aside>
            </section>
          )}
        </div>
      </main>
    </div>
  )
}

function viewLabel(view: View, profile: Profile) {
  if (view === 'expiry' && profile === 'PHARMACY') return 'Medicine Expiry'
  return ({ overview: 'Overview', expiry: 'Expiry Guard', returns: 'Returns', suppliers: 'Supplier insights', assistant: 'Assistant' })[view]
}
function heading(view: View, profile: Profile) {
  if (view === 'expiry' && profile === 'PHARMACY') return 'Protect medicine freshness. Protect patient health.'
  return ({ overview: 'A calmer way to manage stock.', expiry: 'Protect freshness. Protect trust.', returns: 'Listen to what returns tell you.', suppliers: 'Learn from product feedback.', assistant: 'Ask ShopPal about your shop.' })[view]
}
function subtitle(view: View, profile: Profile) {
  if (view === 'expiry' && profile === 'PHARMACY') return 'Spot expiring and expired drug batches before dispensing to patients.'
  return ({ overview: 'See what needs a second look, then decide what to order.', expiry: 'Spot expiring and expired batches before they reach a customer.', returns: 'Use real return records to find product quality patterns.', suppliers: 'Durability signals include sample size and evidence strength.', assistant: 'Get clear answers grounded in this shop’s records.' })[view]
}
function Loading() { return <div className="loading-state"><LoaderCircle className="spin" size={18} /> Loading shop data…</div> }
function Empty({ text }: { text: string }) { return <div className="empty-state"><Check size={17} />{text}</div> }
function PanelTitle({ kicker, title, description }: { kicker: string; title: string; description: string }) { return <div className="panel-heading detail-title"><div><div className="section-kicker">{kicker}</div><h2>{title}</h2><p>{description}</p></div></div> }
