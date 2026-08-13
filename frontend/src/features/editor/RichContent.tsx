"use client";

import { useEditor, EditorContent, type JSONContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import { Table } from "@tiptap/extension-table";
import { TableRow } from "@tiptap/extension-table-row";
import { TableHeader } from "@tiptap/extension-table-header";
import { TableCell } from "@tiptap/extension-table-cell";
import Youtube from "@tiptap/extension-youtube";

const extensions = [StarterKit, Image, Table, TableRow, TableHeader, TableCell,
  Youtube.configure({ controls: true, nocookie: true })];

/** 저장된 블록 문서(JSON)를 읽기 전용으로 렌더한다. */
export function RichContent({ doc }: { doc: JSONContent | null | undefined }) {
  const editor = useEditor({
    extensions,
    content: doc ?? "",
    editable: false,
    immediatelyRender: false,
    editorProps: { attributes: { class: "tiptap-content" } },
  });
  if (!editor) return null;
  return <EditorContent editor={editor} />;
}
