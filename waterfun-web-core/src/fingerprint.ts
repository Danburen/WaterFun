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
    // sessionId: string
    // persistentId: string
}

function collectDeviceFeatures():DeviceFeatures {
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
        // sessionId: getSessionId(),
        // persistentId: getPersistentId()
    }
}

export async function generateFingerprint(): Promise<string> {
    const features = collectDeviceFeatures()
    const featureString = JSON.stringify(features)
    return (await generate256Hash(featureString)).substring(0,16)
}