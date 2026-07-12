import {generate256Hash} from "./simple-cypto";

interface DeviceFeatures {
    screen: {
        width: number
        height: number
        pixelRatio: number
        colorDepth: number
    }
    browser: {
        userAgent: string
        language: string
        cookieEnabled: boolean
    }
    timezone: string
    timezoneOffset: number
    hardwareConcurrency: number | 'unknown'
}

export interface DeviceInfo {
    deviceFp: string
    deviceType: 'PC' | 'MOBILE' | 'TABLET'
    os: string
    browser: string
    screenResolution: string
}

function collectDeviceFeatures(): DeviceFeatures {
    const { screen, navigator } = window

    return {
        screen: {
            width: screen.width,
            height: screen.height,
            pixelRatio: window.devicePixelRatio || 1,
            colorDepth: screen.colorDepth
        },
        browser: {
            userAgent: navigator.userAgent,
            language: navigator.language,
            cookieEnabled: navigator.cookieEnabled
        },
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        timezoneOffset: new Date().getTimezoneOffset(),
        hardwareConcurrency: navigator.hardwareConcurrency || 'unknown',
    }
}

function detectDeviceType(): 'PC' | 'MOBILE' | 'TABLET' {
    const ua = navigator.userAgent
    if (/tablet|ipad|playbook|silk/i.test(ua)) return 'TABLET'
    if (/mobi|iphone|ipod|android.*mobile|blackberry|opera mini/i.test(ua)) return 'MOBILE'
    return 'PC'
}

function detectOS(ua: string): string {
    if (/windows/i.test(ua)) {
        const match = ua.match(/Windows NT (\d+\.?\d*)/)
        if (match) return 'Windows ' + match[1]
        return 'Windows'
    }
    if (/mac os x/i.test(ua)) {
        const match = ua.match(/Mac OS X (\d+[._]\d+[._]?\d*)/)
        if (match) return 'macOS ' + (match[1] || '').replace(/_/g, '.')
        return 'macOS'
    }
    if (/linux/i.test(ua)) return 'Linux'
    if (/android/i.test(ua)) {
        const match = ua.match(/Android (\d+\.?\d*)/)
        if (match) return 'Android ' + match[1]
        return 'Android'
    }
    if (/iphone|ipad|ipod/i.test(ua)) {
        const match = ua.match(/OS (\d+[._]\d+[._]?\d*)/)
        if (match) return 'iOS ' + (match[1] || '').replace(/_/g, '.')
        return 'iOS'
    }
    return 'Unknown'
}

function detectBrowser(ua: string): string {
    if (/chrome/i.test(ua) && !/edge|opr/i.test(ua)) {
        const match = ua.match(/Chrome\/(\d+\.?\d*)/)
        return 'Chrome ' + (match ? match[1] : '')
    }
    if (/firefox/i.test(ua)) {
        const match = ua.match(/Firefox\/(\d+\.?\d*)/)
        return 'Firefox ' + (match ? match[1] : '')
    }
    if (/safari/i.test(ua) && !/chrome|edge|opr/i.test(ua)) {
        const match = ua.match(/Version\/(\d+\.?\d*)/)
        return 'Safari ' + (match ? match[1] : '')
    }
    if (/edge/i.test(ua)) {
        const match = ua.match(/Edge\/(\d+\.?\d*)/)
        return 'Edge ' + (match ? match[1] : '')
    }
    if (/opr/i.test(ua)) {
        const match = ua.match(/OPR\/(\d+\.?\d*)/)
        return 'Opera ' + (match ? match[1] : '')
    }
    return 'Unknown'
}

export async function generateFingerprint(): Promise<string> {
    const features = collectDeviceFeatures()
    const featureString = JSON.stringify(features)
    return (await generate256Hash(featureString)).substring(0,16)
}

export async function getDeviceInfo(): Promise<DeviceInfo> {
    const features = collectDeviceFeatures()
    const ua = features.browser.userAgent
    const fp = await generateFingerprint()

    return {
        deviceFp: fp,
        deviceType: detectDeviceType(),
        os: detectOS(ua),
        browser: detectBrowser(ua),
        screenResolution: features.screen.width + 'x' + features.screen.height,
    }
}
