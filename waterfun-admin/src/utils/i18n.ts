import {createI18n} from "vue-i18n";
import zh from '../../i18n/locales/zh_CN.json'
import en from '../../i18n/locales/en_US.json'

const hasOwn = (obj: unknown, key: string) =>
    obj != null && Object.prototype.hasOwnProperty.call(obj as object, key);

const resolvePath = (obj: unknown, segments: string[]): { found: boolean; value: unknown } => {
    let cur: unknown = obj;
    for (const seg of segments) {
        if (cur != null && typeof cur === 'object' && hasOwn(cur, seg)) {
            cur = (cur as any)[seg];
        } else {
            return { found: false, value: undefined };
        }
    }
    return { found: true, value: cur };
};

// 支持 "not_found" 与 "not_found.args" 这种同前缀 key 共存
const messageResolver = (obj: unknown, path: string): unknown => {
    const segments = path.split('.');

    const direct = resolvePath(obj, segments);
    if (direct.found) return direct.value;

    // fallback: 尝试在最近的可解析父对象上，用剩余段拼成 literal key（包含 '.'）去取值
    for (let split = segments.length - 1; split >= 1; split -= 1) {
        const head = segments.slice(0, split);
        const tailKey = segments.slice(split).join('.');
        const headRes = resolvePath(obj, head);
        if (!headRes.found) continue;
        const parent = headRes.value;
        if (parent != null && typeof parent === 'object' && hasOwn(parent, tailKey)) {
            return (parent as any)[tailKey];
        }
    }

    return undefined;
};

const i18n = createI18n({
    legacy: false,
    locale: 'zh',
    fallbackLocale: 'en',
    messages: {
        zh,
        en
    },
    messageResolver,
    escapeHtml: true
})

export default i18n;