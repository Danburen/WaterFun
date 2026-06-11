import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const md = new MarkdownIt()

function postProcess(html: string): string {
  return html
    .replace(/<img\s/g, '<img loading="lazy" style="max-width:100%;height:auto;display:block;margin:8px 0" ')
    .replace(/<a\s(?![^>]*target=)/g, '<a target="_blank" rel="noopener noreferrer" ')
}

export function renderContent(content: string, format?: string): string {
  if (!content) return ""

  const raw =
    format === "MARKDOWN" ? md.render(content) : content

  return DOMPurify.sanitize(postProcess(raw), {
    ADD_ATTR: ["target", "rel", "loading"],
  })
}
