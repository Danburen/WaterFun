/**
 * Only use in web browser env
 * convert arrayBuff to base64 code
 * @param buffer array buffer
 */
export function convertArrayBufferToBase64(buffer: ArrayBuffer) {
    return btoa(new Uint8Array(buffer).reduce(
        (data, byte) => data + String.fromCharCode(byte), ''
    ));
}

export function base64ToUint8Array(base64: string) {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
}

export async function base64ToText(base64: string) {
    const blob = new Blob([Uint8Array.from(atob(base64), c => c.charCodeAt(0))], {
        type: 'text/plain;charset=utf-8'
    });
    return await new Response(blob).text();
}

// CamelCase transformer
export type ToCamelCase<S extends string> =
    S extends `${infer First}_${infer Rest}`
        ? `${Lowercase<First>}${Capitalize<ToCamelCase<Rest>>}`
        : Lowercase<S>;


