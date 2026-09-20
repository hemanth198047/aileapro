"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import rehypeKatex from "rehype-katex";
import "katex/dist/katex.min.css";

/**
 * Renders authored block content: GitHub-flavoured Markdown plus TeX maths.
 *
 * `rehype-raw` is deliberately absent, so raw HTML in a block is inert. Structure a block
 * needs beyond Markdown (self-check answers, for instance) belongs in its payload and is
 * rendered by React, because later phases let a model draft this content.
 *
 * Display maths must be fenced with `$$` alone on its own line. `$$x$$` on one line renders
 * as inline maths, and text after an opening `$$` is read as an info string and dropped.
 *
 * This module is imported as `@aileapro/ui/markdown` rather than from the package barrel to
 * keep KaTeX out of pages that render no maths.
 */

/** Inline variant for contexts where a block element would be invalid HTML, such as a `<summary>`. */
export function MarkdownInline({ children }: { children: string }) {
  return (
    <span className="md md--inline">
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[rehypeKatex]}
        components={{ p: ({ children: text }) => <>{text}</> }}
      >
        {children}
      </ReactMarkdown>
    </span>
  );
}

export function Markdown({ children }: { children: string }) {
  return (
    <div className="md">
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[rehypeKatex]}
        components={{
          a: ({ href, children: label }) => (
            <a href={href} target="_blank" rel="noreferrer noopener">
              {label}
            </a>
          ),
          table: ({ children: rows }) => (
            <div className="md__scroll">
              <table>{rows}</table>
            </div>
          ),
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}
