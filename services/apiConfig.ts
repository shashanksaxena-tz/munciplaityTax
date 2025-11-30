// Runtime API Configuration - allows users to configure service URLs in test UI
// Configuration is stored in sessionStorage and persists during the browser session

type ApiMode = 'standalone' | 'production';

interface ServiceConfig {
  mode: ApiMode;
  standalone: {
    authService: string;
    ruleService: string;
    taxEngineService: string;
    extractionService: string;
    ledgerService: string;
    submissionService: string;
    pdfService: string;
  };
  production: {
    apiGateway: string;
  };
}

// Default configuration
const DEFAULT_CONFIG: ServiceConfig = {
  mode: 'standalone',
  standalone: {
    authService: 'http://localhost:8081',
    ruleService: 'http://localhost:8084',
    taxEngineService: 'http://localhost:8085',
    extractionService: 'http://localhost:8083',
    ledgerService: 'http://localhost:8087',
    submissionService: 'http://localhost:8089',
    pdfService: 'http://localhost:8086'
  },
  production: {
    apiGateway: 'http://localhost:8080'
  }
};

// Storage key
const STORAGE_KEY = 'munitax_api_config';

// Get current configuration from sessionStorage or use defaults
const getConfig = (): ServiceConfig => {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      return {
        mode: parsed.mode || DEFAULT_CONFIG.mode,
        standalone: { ...DEFAULT_CONFIG.standalone, ...parsed.standalone },
        production: { ...DEFAULT_CONFIG.production, ...parsed.production }
      };
    }
  } catch (e) {
    console.warn('Failed to load API config from storage:', e);
  }
  return DEFAULT_CONFIG;
};

// Save configuration to sessionStorage
const saveConfig = (config: Partial<ServiceConfig>) => {
  try {
    const current = getConfig();
    const updated: ServiceConfig = {
      mode: config.mode || current.mode,
      standalone: { ...current.standalone, ...config.standalone },
      production: { ...current.production, ...config.production }
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  } catch (e) {
    console.error('Failed to save API config:', e);
  }
};

// Get service URL based on current configuration
const getServiceUrl = (servicePath: string): string => {
  const config = getConfig();
  
  if (config.mode === 'standalone') {
    // Map service paths to configured URLs
    if (servicePath.startsWith('/auth') || servicePath.startsWith('/api/v1/auth') || servicePath.startsWith('/api/v1/users')) {
      return config.standalone.authService;
    }
    if (servicePath.startsWith('/rules')) {
      return config.standalone.ruleService;
    }
    if (servicePath.startsWith('/tax-engine')) {
      return config.standalone.taxEngineService;
    }
    if (servicePath.startsWith('/extraction')) {
      return config.standalone.extractionService;
    }
    if (servicePath.startsWith('/ledger')) {
      return config.standalone.ledgerService;
    }
    if (servicePath.startsWith('/submission')) {
      return config.standalone.submissionService;
    }
    if (servicePath.startsWith('/pdf')) {
      return config.standalone.pdfService;
    }
    return config.standalone.authService;
  } else {
    // Production mode - all through API Gateway
    return config.production.apiGateway;
  }
};

export const apiConfig = {
  // Get current configuration
  getConfig,
  
  // Save configuration (partial update)
  saveConfig,
  
  // Reset to defaults
  resetConfig: () => {
    sessionStorage.removeItem(STORAGE_KEY);
  },
  
  // Get service URL for a path
  getServiceUrl,
  
  // Build full URL
  buildUrl: (servicePath: string, endpoint: string): string => {
    const baseUrl = getServiceUrl(servicePath);
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    return `${baseUrl}${cleanEndpoint}`;
  },
  
  // Set mode
  setMode: (mode: ApiMode) => {
    saveConfig({ mode });
  },
  
  // Update standalone service URL
  updateStandaloneService: (service: keyof ServiceConfig['standalone'], url: string) => {
    const config = getConfig();
    saveConfig({
      standalone: {
        ...config.standalone,
        [service]: url
      }
    });
  },
  
  // Update production gateway URL
  updateProductionGateway: (url: string) => {
    saveConfig({
      production: { apiGateway: url }
    });
  },
  
  // Get default config (for reset/comparison)
  getDefaults: () => DEFAULT_CONFIG
};
