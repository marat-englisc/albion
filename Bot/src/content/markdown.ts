import { marked, type Token, type Tokens } from "marked";
import { escapeHtml } from "../utils.js";

function inline(source: string): string {
  const render = (tokens: Token[]): string =>
    tokens
      .map((t): string => {
        switch (t.type) {
          case "strong":
            return `<b>${render(t.tokens ?? [])}</b>`;
          case "em":
            return `<i>${render(t.tokens ?? [])}</i>`;
          case "del":
            return `<s>${render(t.tokens ?? [])}</s>`;
          case "codespan":
            return `<code>${escapeHtml(t.text)}</code>`;
          case "br":
            return "\n";
          case "link":
            return /^https?:\/\//i.test(t.href)
              ? `<a href="${escapeHtml(t.href)}">${render(t.tokens ?? [])}</a>`
              : render(t.tokens ?? []);
          default:
            return escapeHtml("text" in t ? String(t.text) : t.raw);
        }
      })
      .join("");
  return render(
    marked
      .lexer(source, { breaks: true })
      .flatMap((t) =>
        "tokens" in t && t.tokens
          ? t.tokens
          : ([{ type: "text", raw: source, text: source }] as Token[]),
      ),
  );
}

export function cleanObsidian(source: string): string {
  return source
    .replace(
      /\[\[([^\]|]+)(?:\|([^\]]+))?\]\]/g,
      (_all, target: string, label?: string) => label ?? target,
    )
    .replace(/==(.+?)==/g, "**$1**")
    .replace(/^(\s*>\s*)\[![\w-]+\][+-]?\s*(.*)$/gm, "$1**$2**");
}

export function lessonSections(source: string): {
  main: string;
  answers: string;
} {
  const answerBlock =
    /(^>\s*\[!success\][+-]?\s*Ответы[^\n]*\n(?:>[^\n]*(?:\n|$))*)/m;
  const match = answerBlock.exec(source);
  return {
    main: source.replace(
      answerBlock,
      "> Ответы доступны по кнопке «Ответы к уроку».\n",
    ),
    answers: match?.[0] ?? "",
  };
}

export function markdownPages(markdown: string, maxLength = 3400): string[] {
  const blocks: string[] = [];
  const render = (tokens: Token[]) => {
    for (const t of tokens) {
      switch (t.type) {
        case "heading":
          blocks.push(`<b>${inline(t.text)}</b>`);
          break;
        case "paragraph":
          blocks.push(inline(t.text));
          break;
        case "blockquote":
          render(t.tokens ?? []);
          break;
        case "list":
          for (const [i, item] of t.items.entries())
            blocks.push(
              `${t.ordered ? `${Number(t.start) + i}.` : "•"} ${inline(item.text)}`,
            );
          break;
        case "table":
          for (const row of (t as Tokens.Table).rows)
            blocks.push(
              row
                .map(
                  (c, i) =>
                    `${inline(t.header[i]?.text ?? "")}: ${inline(c.text)}`,
                )
                .join("\n"),
            );
          break;
        case "code":
          blocks.push(`<pre>${escapeHtml(t.text)}</pre>`);
          break;
        case "hr":
          blocks.push("──────────");
          break;
        case "text":
          blocks.push(inline(t.text));
          break;
        case "html":
          blocks.push(escapeHtml(t.text));
          break;
      }
    }
  };

  render(marked.lexer(cleanObsidian(markdown)));

  const pages: string[] = [];

  let current = "";

  for (let block of blocks) {
    if (block.length > maxLength) {
      // Oversized blocks fall back to escaped text chunks, never splitting HTML tags/entities.
      const plain = block
        .replace(/<[^>]*>/g, "")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&quot;/g, '"')
        .replace(/&amp;/g, "&");
      const chunks = Array.from(plain).reduce<string[]>((all, char) => {
        const escaped = escapeHtml(char);
        const last = all.length - 1;
        if (last < 0 || all[last]!.length + escaped.length > maxLength)
          all.push(escaped);
        else all[last] += escaped;
        return all;
      }, []);
      if (current) {
        pages.push(current);
        current = "";
      }
      pages.push(...chunks.slice(0, -1));
      block = chunks.at(-1) ?? "";
    }
    if (current && current.length + block.length + 2 > maxLength) {
      pages.push(current);
      current = "";
    }
    current += `${current ? "\n\n" : ""}${block}`;
  }
  if (current) pages.push(current);
  return pages.length ? pages : ["Урок пуст."];
}
