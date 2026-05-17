declare module 'json-bigint' {
  interface JSONBigOptions {
    storeAsString?: boolean;
    useNativeBigInt?: boolean;
    alwaysParseAsBig?: boolean;
  }

  interface JSONBigStatic {
    parse(text: string): any;
    stringify(value: any): string;
  }

  export default function JSONBig(options?: JSONBigOptions): JSONBigStatic;
}
