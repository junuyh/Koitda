"use client";

import { useEditor, EditorContent, type Editor, type JSONContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import { Table } from "@tiptap/extension-table";
import { TableRow } from "@tiptap/extension-table-row";
import { TableHeader } from "@tiptap/extension-table-header";
import { TableCell } from "@tiptap/extension-table-cell";
import { useRef } from "react";
import { fileApi, type UploadUsage } from "@/features/file/api";

const extensions = [
  StarterKit,
  Image.configure({ inline: false }),
  Table.configure({ resizable: false }),
  TableRow,
  TableHeader,
  TableCell,
];

export type RichValue = { json: JSONContent; text: string };

/** 블록 에디터(줄글·목록·사진·표). 이미지는 파일 업로드로 저장하고 서빙 URL 을 넣는다. */
export function RichEditor({
  initial, onChange, usageType = "PATTERN_IMAGE", placeholder,
}: {
  initial?: JSONContent | null;
  onChange: (v: RichValue) => void;
  usageType?: UploadUsage;
  placeholder?: string;
}) {
  const fileRef = useRef<HTMLInputElement>(null);

  const editor = useEditor({
    extensions,
    content: initial ?? "",
    immediatelyRender: false,
    editorProps: { attributes: { class: "tiptap-content min-h-[160px] px-3 py-2 outline-none" } },
    onUpdate: ({ editor }) => onChange({ json: editor.getJSON(), text: editor.getText() }),
  });

  if (!editor) {
    return <div className="rounded-lg border border-neutral-300 p-3 text-sm text-neutral-400 dark:border-neutral-700">에디터 불러오는 중…</div>;
  }

  async function onPickImage(files: FileList | null) {
    if (!files || !files[0] || !editor) return;
    try {
      const res = await fileApi.upload(files[0], usageType);
      editor.chain().focus().setImage({ src: `/api/v1/files/${res.id}` }).run();
    } catch {
      window.alert("이미지 업로드에 실패했습니다.");
    } finally {
      if (fileRef.current) fileRef.current.value = "";
    }
  }

  return (
    <div className="rounded-lg border border-neutral-300 dark:border-neutral-700">
      <Toolbar editor={editor} onImage={() => fileRef.current?.click()} placeholder={placeholder} />
      <EditorContent editor={editor} />
      <input ref={fileRef} type="file" accept="image/*" hidden onChange={(e) => onPickImage(e.target.files)} />
    </div>
  );
}

function Toolbar({ editor, onImage }: { editor: Editor; onImage: () => void; placeholder?: string }) {
  const btn = "rounded px-2 py-1 text-xs hover:bg-neutral-100 dark:hover:bg-neutral-800";
  const active = "bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-900";
  return (
    <div className="flex flex-wrap items-center gap-1 border-b border-neutral-200 px-2 py-1.5 dark:border-neutral-800">
      <button type="button" className={`${btn} ${editor.isActive("heading", { level: 2 }) ? active : ""}`}
        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}>제목</button>
      <button type="button" className={`${btn} font-bold ${editor.isActive("bold") ? active : ""}`}
        onClick={() => editor.chain().focus().toggleBold().run()}>B</button>
      <button type="button" className={`${btn} italic ${editor.isActive("italic") ? active : ""}`}
        onClick={() => editor.chain().focus().toggleItalic().run()}>I</button>
      <button type="button" className={`${btn} ${editor.isActive("bulletList") ? active : ""}`}
        onClick={() => editor.chain().focus().toggleBulletList().run()}>• 목록</button>
      <button type="button" className={`${btn} ${editor.isActive("orderedList") ? active : ""}`}
        onClick={() => editor.chain().focus().toggleOrderedList().run()}>1. 목록</button>
      <span className="mx-1 h-4 w-px bg-neutral-200 dark:bg-neutral-700" />
      <button type="button" className={btn} onClick={onImage}>🖼 사진</button>
      <button type="button" className={btn}
        onClick={() => editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()}>▦ 표</button>
    </div>
  );
}
