package dev.levia.crosswordsolver2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageActivity;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main extends Activity {
    private Models model_db;
    private ListView search_list;
    private EditText search_input;
    private String crossword_name;
    private ActivityResultLauncher<String> getContent;
    private final List<String> list_input = List.of( "checked", "solved", "fixed", "SOME", "Random", "values", "that", "doesnt", "make", "any", "sense", "just", "check", "the", "last", "words", "in", "alphabeth" );

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        model_db = new Models(Main.this);
        var crosswords = new ArrayList<>(model_db.getCrosswords().stream().map(Models.CrosswordInternalModel::getName).collect(Collectors.toList()));

        search_list = findViewById(R.id.search_main_list);
        search_list.setAdapter(new ArrayAdapter<>(Main.this, R.layout.row, R.id.row_text, crosswords));
        search_list.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            var menu = new PopupMenu(Main.this, view);
            menu.getMenuInflater().inflate(R.menu.removed, menu.getMenu());
            menu.setOnMenuItemClickListener((MenuItem item) -> {
                if (item.getItemId() == 0) {
                    model_db.delCrossword(crosswords.get(position));
                }
                Log.d("EMPTY", "MENU ID IS NOT IMPLEMENTED "+item.getItemId());
                return false;
            });
        });
        search_list.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            Log.i("EMPTY", String.format("SELECTED ITEM IS AT %d WITH VALUE %s", position, selectedItem));
            var intent = new Intent(Main.this, CrosswordVisual.class);
            intent.putExtra("crossword_name", selectedItem);
            startActivity(intent);
        });
        Log.d("EMPTY", String.format("COUNT ADAPTER: %d", search_list.getAdapter().getCount()));
        // search_list.getAdapter().get

        FloatingActionButton fab_btn = findViewById(R.id.fab);
        fab_btn.setOnClickListener((View v) -> {
            var new_name = new EditText(Main.this);
            askForInput("Crossword name?", "CONFIRM", (DialogInterface dialog, int btn) -> {
                Intent intent = new Intent(Main.this, CropImageActivity.class);
                startActivityForResult(intent, 101);
                Log.d("EMPTY", new_name.getText().toString());
                crossword_name = new_name.getText().toString();
                ((ArrayAdapter<String>) search_list.getAdapter()).add(crossword_name);
            }, new_name);
        });

        search_input = findViewById(R.id.search_main_input);
        var listener = new Listeners.OnChangeListener();
        listener.setAdapter((ArrayAdapter<String>) search_list.getAdapter());
        search_input.addTextChangedListener(listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        CropImage.ActivityResult result = data.getParcelableExtra(CropImage.CROP_IMAGE_EXTRA_RESULT);
        // TODO now you have uri of image get content of it
        Log.d("EMPTY", String.format("%s", result.getUriContent().toString()));
        var detect = new TextRecognizer.Builder(Main.this).build();
        try {
            var bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), result.getUriContent());
            var frame = new Frame.Builder().setBitmap(bitmap).build();

            var items = detect.detect(frame);
            var sb = new StringBuilder();
            var list = new ArrayList<String>();
            for (int i = 0; i < items.size(); i++) {
                Log.d("EMPTY", String.format("VALUES: %s", items.valueAt(i).getValue()));
                // list.add(items.valueAt(i).getValue().replaceAll("[^a-zA-Z]", ""));
                sb.append(items.valueAt(i).getValue().replaceAll(getString(R.string.ONLY_CHARACTERS), ""));
            }
            var new_name = new EditText(Main.this);
            var new_content = new EditText(Main.this);
            askForInput("HOW MANY CHARACTER ARE IN WIDTH?", "CONFIRM", (DialogInterface dialog, int btn) -> {
                new_name.requestFocus();
                new_name.setInputType(InputType.TYPE_CLASS_NUMBER);

                var count_str = new_name.getText().toString().replaceAll(getString(R.string.ONLY_NUMBERS), "");
                var count = Integer.parseInt(count_str);
                Log.d("EMPTY", new_name.getText().toString());
                // Toast.makeText(this, String.format("Crossword has %s", count), Toast.LENGTH_SHORT).show();

                // TODO `sb` split to list with every line max size of `count`
                var index = 0;
                final var length = sb.length();
                while (index <= length) {
                    list.add(sb.substring(index, Math.min(index + count, length)).toUpperCase());
                    index += count;
                }

                new_content.setText(String.join("\n", list));
                new_content.setFilters(new InputFilter[] {new InputFilter.AllCaps()});

                askForInput("Is This correct? ", "CONFIRM", (DialogInterface dialog2, int btn2) -> {
                    var intent = new Intent(Main.this, CrosswordVisual.class);
                    intent.putExtra("crossword_name", crossword_name);
                    model_db.addCrossword(crossword_name, new_content.getText().toString());
                    startActivity(intent);
                }, new_content);
            }, new_name);


            // var len = list.stream().mapToInt(String::length).max().orElse(0);
            // var fixed_list = new ArrayList<String>();
            // var index = 0;
            // for (int i = 0; i < list.size(); i++) {
            //     if (list.get(i).length() != len) {
            //         fixed_list.set(index, list.get(i));
            //     } else {
            //         index++;
            //     }
            // }

            // Log.d("EMPTY", String.format("AFTER OCR: %s", fixed_list.stream().collect(Collectors.joining("\n"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Bitmap.createBitmap(result.getUriContent());

        super.onActivityResult(requestCode, resultCode, data);
    }
    public void askForInput(String title, String confirm, DialogInterface.OnClickListener listener, View view) {
        new AlertDialog.Builder(Main.this).setTitle(title).setView(view)
                .setPositiveButton(confirm, listener)
                .setNegativeButton("CANCEL", (DialogInterface dialog, int btn) -> {
                    Log.d("EMPTY", "Cancel pressed in dialog");
                }).show();
    }
}
