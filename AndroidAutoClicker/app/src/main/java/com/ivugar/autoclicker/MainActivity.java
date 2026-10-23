package com.ivugar.autoclicker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Экран настроек: интервал, число кликов, включение сервиса. */
public class MainActivity extends Activity {

    static final String PREFS = "cfg";
    static final String KEY_INTERVAL = "interval";
    static final String KEY_LIMIT = "limit";
    static final int MIN_INTERVAL = 20;

    private TextView status;
    private Button overlayBtn;
    private EditText intervalEdit, limitEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Своя шапка вместо ActionBar: на Android 15 окно рисуется под системными панелями,
        // поэтому отступы берём из WindowInsets.
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(this);
        header.setText(R.string.app_name);
        header.setTextSize(22);
        header.setTextColor(Color.WHITE);
        header.setBackgroundColor(0xFF212121);
        header.setPadding(pad, pad, pad, pad);
        screen.addView(header);

        ScrollView scroll = new ScrollView(this);
        screen.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        screen.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom, left, right;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                top = i.top; bottom = i.bottom; left = i.left; right = i.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
            }
            header.setPadding(pad + left, pad + top, pad + right, pad);
            scroll.setPadding(left, 0, right, bottom);
            return insets;
        });

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, pad);
        root.addView(status);

        root.addView(label("Интервал между кликами, мс (мин. " + MIN_INTERVAL + "):"));
        intervalEdit = number(prefs.getInt(KEY_INTERVAL, 500));
        root.addView(intervalEdit);

        root.addView(label("Сколько кликов сделать (0 = бесконечно):"));
        limitEdit = number(prefs.getInt(KEY_LIMIT, 0));
        root.addView(limitEdit);

        Button save = new Button(this);
        save.setText("Сохранить");
        save.setOnClickListener(v -> save());
        root.addView(save);

        Button access = new Button(this);
        access.setText("Включить в «Спец. возможностях»");
        access.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);

        overlayBtn = new Button(this);
        overlayBtn.setText("Показать панель");
        overlayBtn.setOnClickListener(v -> {
            save();
            if (ClickService.instance != null) {
                ClickService.instance.showOverlay();
                moveTaskToBack(true);
            }
        });
        root.addView(overlayBtn);

        TextView help = label("\nКак пользоваться:\n"
                + "1. Включите «Автокликер» в Спец. возможностях.\n"
                + "2. Перетащите красный прицел туда, где нужно нажимать.\n"
                + "3. Нажмите ▶ на панели. ■ — стоп, ✥ — перетащить панель, ✕ — скрыть.\n\n"
                + "Если на Android 13+ переключатель серый: Настройки → Приложения → "
                + "Автокликер → ⋮ → «Разрешить доступ к настройкам ограниченного доступа».");
        root.addView(help);

        setContentView(screen);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = ClickService.instance != null;
        status.setText(on ? "Сервис включён ✔" : "Сервис выключен — включите его в Спец. возможностях");
        overlayBtn.setEnabled(on);
    }

    private void save() {
        int interval = parse(intervalEdit, 500);
        if (interval < MIN_INTERVAL) {
            interval = MIN_INTERVAL;
            intervalEdit.setText(String.valueOf(interval));
        }
        int limit = Math.max(0, parse(limitEdit, 0));
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_INTERVAL, interval)
                .putInt(KEY_LIMIT, limit)
                .apply();
        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
    }

    private int parse(EditText e, int def) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        return t;
    }

    private EditText number(int value) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(value));
        return e;
    }
}
