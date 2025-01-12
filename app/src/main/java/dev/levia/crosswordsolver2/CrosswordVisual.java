package dev.levia.crosswordsolver2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextPaint;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CrosswordVisual extends Activity implements SurfaceHolder.Callback {
    private Models model_db;
    private Models.CrosswordInternalModel crossword;
    private CrosswordSolver solver;
    private SurfaceView surface;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crossword_visual);

        FloatingActionButton button = findViewById(R.id.floatingActionButton);

        model_db = new Models(CrosswordVisual.this);
        ListView list = findViewById(R.id.list_items);
        var item_name = (String) Objects.requireNonNull(getIntent().getExtras()).get("crossword_name");
        crossword = model_db.getCrossword(item_name);

        list.setAdapter(new ArrayAdapter<>(CrosswordVisual.this, R.layout.row, R.id.row_text, crossword.getWords()));
        list.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id)  -> {
            var menu = new PopupMenu(CrosswordVisual.this, view);
            menu.getMenuInflater().inflate(R.menu.removed, menu.getMenu());
            menu.setOnMenuItemClickListener((MenuItem item) -> {
                if (item.getItemId() == 0) {
                    var name = (Models.CrosswordInternalModel) parent.getItemAtPosition(position);
                    model_db.delCrossword(name.getName());
                } else {
                    Log.d("EMPTY", "MENU ID IS NOT IMPLEMENTED "+item.getItemId());
                }
                return false;
            });
            redraw(position);
        });
        button.setOnClickListener((View v) -> {
            var text = new TextInputEditText(CrosswordVisual.this);
            text.setInputType(InputType.TYPE_CLASS_TEXT);
            text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            text.setFilters(new InputFilter[] {new InputFilter.AllCaps()});

            AlertDialog.Builder b = new AlertDialog.Builder(CrosswordVisual.this);
            b.setView(text);
            b.setTitle("new word");
            b.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
                var name = Objects.requireNonNull(text.getText()).toString();
                crossword.addWord(name);
                redraw(0);
                model_db.updateModel(crossword);
                ((ArrayAdapter<String>) list.getAdapter()).add(name);
                Log.d("EMPTY", String.format("WORDS_TOTAL: %d", crossword.getWords().size()));
            });
            b.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
            b.show();
        });
        surface = findViewById(R.id.gl_canvas);
        surface.setZOrderOnTop(true);
        surface.getHolder().setFormat(PixelFormat.TRANSPARENT);
        surface.getHolder().addCallback(this);

        solver = new CrosswordSolver(List.of(crossword.getContent().split(getString(R.string.new_line))));
        redraw(0);
    }
    private static final int FONT_SIZE = 100;
    public void redraw(int position) {
        if (!surface.getHolder().getSurface().isValid()) return;
        var canvas = surface.getHolder().lockCanvas();
        canvas.drawColor(Color.BLACK);
        var font_size = (canvas.getWidth()*9)/(crossword.getContent().split(getString(R.string.new_line), 2)[0].length() * 10);
        Log.d("EMPTY", String.format("%d => %d %d", canvas.getWidth(), font_size, (crossword.getContent().split(getString(R.string.new_line), 1)[0].length())));
        var count = 0;
        var paint = new TextPaint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(font_size);
        paint.setTextAlign(Paint.Align.CENTER);

        for (var line : crossword.getContent().split(getString(R.string.new_line))) {
            for (int i = 0; i < line.length(); i++) {
                canvas.drawText(String.valueOf(line.charAt(i)), font_size + (font_size*i), font_size+(count*font_size), paint);
            }
            count++;
        }

        var paint2 = getPainter(50, 0, 0, 0, font_size);
        var paint3 = getPainter(100, 255, 0, 0, font_size);
        var paint4 = getPainter(100, 0, 0, 255, font_size);

        Log.d("EMPTY", String.format("POSITION: %d", position));
        var words = crossword.getWords();
        for (int i = 0; i < words.size(); i++) {
            var ans = solver.find(words.get(i));
            Log.d("EMPTY", String.format("ans: %d %s", ans.size(), words.get(i)));
            for (int j = 0; j < ans.size(); j++) {
                var point = ans.get(j);
                var origin = point.getOrigin((short) words.get(i).length());

                var offset_ax = (font_size * point.ox())/2 ;
                var offset_ay = (font_size * point.oy())/2 ;
                var offset_bx = (font_size * origin.ox())/2*-1;
                var offset_by = (font_size * origin.oy())/2*-1;
                var off_x = font_size/10;
                var ax = font_size + font_size*(origin.y() > point.y() ? origin.y() : point.y())+offset_ay;
                var ay = font_size/2+font_size*(origin.x() > point.x() ? origin.x() : point.x())+offset_ax + off_x;
                var bx = font_size + font_size*(origin.y() < point.y() ? origin.y() : point.y())+offset_by;
                var by = font_size/2+font_size*(origin.x() < point.x() ? origin.x() : point.x())+offset_bx + off_x;
                canvas.drawLine(
                        ax, ay, bx, by,
                        position != 0 && position == i ? paint3 : paint2
                );
                if (position != 0 && position == i) {
                    canvas.drawCircle(ax, ay, 10, paint3);
                    canvas.drawCircle(bx, by, 10, paint4);
                }
            }
        }

        surface.getHolder().unlockCanvasAndPost(canvas);
    }
    public Paint getPainter(int alpha, int red, int green, int blue, int size) {
        var paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(alpha, red, green, blue));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStrokeWidth(size);
        return paint;
    }

    @Override public void onPointerCaptureChanged(boolean hasCapture) { super.onPointerCaptureChanged(hasCapture); }
    @Override public void surfaceCreated(@NonNull SurfaceHolder holder) { }
    @Override public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { }
    @Override public void surfaceDestroyed(@NonNull SurfaceHolder holder) { }
}
