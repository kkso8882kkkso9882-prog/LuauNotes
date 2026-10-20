package com.kkso.luaunotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity {
    static final int BG = 0xFF121212, FG = 0xFFEEEEEE, ACC = 0xFF4FC3F7, DIM = 0xFF9E9E9E;

    private SharedPreferences sp;
    private final ArrayList<JSONObject> notes = new ArrayList<>();
    private boolean inEditor = false;
    private JSONObject current;
    private EditText nameEt, codeEt;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("luau_notes", MODE_PRIVATE);
        load();
        showList();
    }

    void load() {
        notes.clear();
        try {
            JSONArray a = new JSONArray(sp.getString("notes", "[]"));
            for (int i = 0; i < a.length(); i++) notes.add(a.getJSONObject(i));
        } catch (Exception ignored) { }
    }

    void persist() {
        JSONArray a = new JSONArray();
        for (JSONObject o : notes) a.put(o);
        sp.edit().putString("notes", a.toString()).apply();
    }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(BG);
        l.setPadding(dp(12), dp(36), dp(12), dp(8));
        return l;
    }

    Button btn(String t, View.OnClickListener c) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setOnClickListener(c);
        return b;
    }

    EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(FG);
        e.setHintTextColor(DIM);
        return e;
    }

    // ---------- หน้ารายการ ----------
    void showList() {
        inEditor = false;
        LinearLayout root = col();
        TextView title = new TextView(this);
        title.setText("📜 Luau Notes");
        title.setTextColor(ACC);
        title.setTextSize(22);
        title.setPadding(0, 0, 0, dp(8));
        final EditText search = edit("ค้นหาชื่อโน้ต...");
        search.setSingleLine(true);
        final ListView lv = new ListView(this);
        Button add = btn("+ สร้างโน้ตใหม่", v -> {
            current = new JSONObject();
            showEditor(true);
        });
        root.addView(title);
        root.addView(search);
        root.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(add);
        setContentView(root);
        fill(lv, "");
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { fill(lv, s.toString()); }
            public void afterTextChanged(Editable s) { }
        });
    }

    void fill(ListView lv, String q) {
        final ArrayList<JSONObject> view = new ArrayList<>();
        for (JSONObject o : notes)
            if (q.isEmpty() || o.optString("name").toLowerCase().contains(q.toLowerCase())) view.add(o);
        Collections.sort(view, (a, b) -> Long.compare(b.optLong("updated"), a.optLong("updated")));
        ArrayList<String> labels = new ArrayList<>();
        for (JSONObject o : view) {
            int lines = o.optString("code").isEmpty() ? 0 : o.optString("code").split("\n").length;
            labels.add(o.optString("name") + "\n" + lines + " บรรทัด");
        }
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                TextView t = (TextView) super.getView(pos, cv, parent);
                t.setTextColor(FG);
                return t;
            }
        });
        lv.setOnItemClickListener((p, v, pos, id) -> {
            current = view.get(pos);
            showEditor(false);
        });
        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            menu(view.get(pos));
            return true;
        });
    }

    void menu(final JSONObject n) {
        new AlertDialog.Builder(this)
                .setTitle(n.optString("name"))
                .setItems(new String[]{"เปลี่ยนชื่อ", "ทำสำเนา", "ลบ"}, (d, which) -> {
                    try {
                        if (which == 0) {
                            final EditText e = new EditText(this);
                            e.setText(n.optString("name"));
                            new AlertDialog.Builder(this).setTitle("ตั้งชื่อใหม่").setView(e)
                                    .setPositiveButton("ตกลง", (x, y) -> {
                                        try {
                                            n.put("name", e.getText().toString().trim());
                                            n.put("updated", System.currentTimeMillis());
                                        } catch (Exception ignored) { }
                                        persist();
                                        showList();
                                    }).setNegativeButton("ยกเลิก", null).show();
                        } else if (which == 1) {
                            JSONObject c = new JSONObject(n.toString());
                            c.put("name", n.optString("name") + " (สำเนา)");
                            c.put("updated", System.currentTimeMillis());
                            notes.add(c);
                            persist();
                            showList();
                        } else {
                            new AlertDialog.Builder(this).setTitle("ลบโน้ตนี้?")
                                    .setPositiveButton("ลบ", (x, y) -> {
                                        notes.remove(n);
                                        persist();
                                        showList();
                                    }).setNegativeButton("ยกเลิก", null).show();
                        }
                    } catch (Exception ignored) { }
                }).show();
    }

    // ---------- หน้าแก้ไข ----------
    void showEditor(boolean isNew) {
        inEditor = true;
        LinearLayout root = col();
        nameEt = edit("ชื่อสคริป");
        nameEt.setSingleLine(true);
        nameEt.setTextSize(18);
        nameEt.setText(current.optString("name"));
        codeEt = edit("-- วางสคริป Luau ที่นี่");
        codeEt.setTypeface(Typeface.MONOSPACE);
        codeEt.setTextSize(13);
        codeEt.setGravity(Gravity.TOP | Gravity.START);
        codeEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        codeEt.setHorizontallyScrolling(true);
        codeEt.setText(current.optString("code"));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(btn("บันทึก", v -> { save(); Toast.makeText(this, "บันทึกแล้ว", Toast.LENGTH_SHORT).show(); }), lp);
        row.addView(btn("คัดลอก", v -> {
            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("luau", codeEt.getText().toString()));
            Toast.makeText(this, "คัดลอกแล้ว", Toast.LENGTH_SHORT).show();
        }), lp);
        row.addView(btn("วาง", v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                codeEt.getText().insert(codeEt.getSelectionStart(), t);
            }
        }), lp);
        row.addView(btn("แชร์", v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, codeEt.getText().toString());
            startActivity(Intent.createChooser(i, "แชร์สคริป"));
        }), lp);
        row.addView(btn("กลับ", v -> { save(); showList(); }), lp);

        root.addView(nameEt);
        root.addView(codeEt, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(row);
        setContentView(root);
    }

    void save() {
        try {
            String name = nameEt.getText().toString().trim();
            String code = codeEt.getText().toString();
            boolean exists = notes.contains(current);
            if (!exists && name.isEmpty() && code.isEmpty()) return;
            if (name.isEmpty()) name = "ไม่มีชื่อ";
            current.put("name", name);
            current.put("code", code);
            current.put("updated", System.currentTimeMillis());
            if (!exists) notes.add(current);
            persist();
        } catch (Exception ignored) { }
    }

    @Override
    public void onBackPressed() {
        if (inEditor) { save(); showList(); }
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (inEditor) save();
    }
}
