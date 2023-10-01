class HitCounter extends HTMLElement {
    constructor() {
        super();
    }

    async connectedCallback() {
        const uri = this.getAttribute('uri');
        if (!this.isValidURI(uri)) {
            return;
        }
        await this.fetchAndUpdateHitCounter(uri);
    }

    isValidURI(uri) {
        return uri !== null && uri.trim() !== '';
    }

    async fetchAndUpdateHitCounter(uri) {
        try {
            const hitCounterValue = await this.fetchHitCounter(uri);
            this.textContent = hitCounterValue;
        } catch (error) {
            console.error('Error fetching and updating hit counter:', error);
        }
    }

    async fetchHitCounter(uri) {
        try {
            const response = await fetch(uri);
            if (!response.ok) {
                throw new Error('Failed to retrieve hit counter');
            }
            return await response.text();
        } catch (error) {
            return '0';
        }
    }
}

customElements.define('visitor-number', HitCounter);