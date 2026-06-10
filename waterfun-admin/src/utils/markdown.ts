import { marked } from "marked";
import DOMPurify from "dompurify";

const renderer = new marked.Renderer();

renderer.image = ({ href, title, text }) => {
  const alt = text.replace(/["<>]/g, "");
  return `<img src="${href}" alt="${alt}"${title ? ` title="${title.replace(/["<>]/g, "")}"` : ""} loading="lazy" style="max-width:100%;height:auto;display:block;margin:8px 0" />`;
};

const origLink = renderer.link.bind(renderer);
renderer.link = ({ href, title, text }) => {
  const html = origLink({ href, title, text });
  return html.replace("<a", '<a target="_blank" rel="noopener noreferrer"');
};

marked.use({ renderer, breaks: true, gfm: true });

export function renderContent(content: string, format?: string): string {
  if (!content) return "";

  const raw =
    format === "MARKDOWN" ? marked.parse(content, { async: false }) : content;

  return DOMPurify.sanitize(typeof raw === "string" ? raw : "", {
    ADD_ATTR: ["target", "rel", "loading"],
  });
}
