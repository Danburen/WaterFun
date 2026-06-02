declare interface ImportMeta {
  readonly client: boolean;
}

declare module '*.css' {
    const css: string
    export default css
}