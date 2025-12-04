/**
 * Safe storage wrapper that handles cases where localStorage/sessionStorage
 * is not available (e.g., private browsing, iframe restrictions, browser settings)
 */

class SafeStorage {
    private memoryStorage: Map<string, string> = new Map();
    private storage: Storage | null = null;
    private isAvailable: boolean = false;

    constructor(private storageType: 'localStorage' | 'sessionStorage' = 'localStorage') {
        this.initStorage();
    }

    private initStorage(): void {
        try {
            // Get the storage reference safely
            const storageObj = this.storageType === 'localStorage' 
                ? (typeof window !== 'undefined' ? window.localStorage : null)
                : (typeof window !== 'undefined' ? window.sessionStorage : null);
            
            if (!storageObj) {
                console.warn(`${this.storageType} is not available (no window object), using memory storage`);
                return;
            }

            // Test if we can actually use it
            const testKey = '__storage_test__';
            storageObj.setItem(testKey, 'test');
            storageObj.removeItem(testKey);
            
            // If we got here, storage works
            this.storage = storageObj;
            this.isAvailable = true;
        } catch (e) {
            console.warn(`${this.storageType} is not available, using memory storage fallback:`, e);
            this.storage = null;
            this.isAvailable = false;
        }
    }

    getItem(key: string): string | null {
        // Try storage first if available
        if (this.isAvailable && this.storage) {
            try {
                return this.storage.getItem(key);
            } catch (e) {
                console.warn(`Error reading from ${this.storageType}, falling back to memory:`, e);
                this.isAvailable = false;
            }
        }
        
        // Fall back to memory
        return this.memoryStorage.get(key) || null;
    }

    setItem(key: string, value: string): void {
        // Always store in memory as backup
        this.memoryStorage.set(key, value);
        
        // Try to store in actual storage if available
        if (this.isAvailable && this.storage) {
            try {
                this.storage.setItem(key, value);
            } catch (e) {
                console.warn(`Error writing to ${this.storageType}, using memory only:`, e);
                this.isAvailable = false;
            }
        }
    }

    removeItem(key: string): void {
        // Remove from memory
        this.memoryStorage.delete(key);
        
        // Try to remove from actual storage if available
        if (this.isAvailable && this.storage) {
            try {
                this.storage.removeItem(key);
            } catch (e) {
                console.warn(`Error removing from ${this.storageType}:`, e);
                this.isAvailable = false;
            }
        }
    }

    clear(): void {
        // Clear memory
        this.memoryStorage.clear();
        
        // Try to clear actual storage if available
        if (this.isAvailable && this.storage) {
            try {
                this.storage.clear();
            } catch (e) {
                console.warn(`Error clearing ${this.storageType}:`, e);
                this.isAvailable = false;
            }
        }
    }
}

// Export singleton instances
export const safeLocalStorage = new SafeStorage('localStorage');
export const safeSessionStorage = new SafeStorage('sessionStorage');
