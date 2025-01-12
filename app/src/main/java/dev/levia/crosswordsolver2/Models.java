package dev.levia.crosswordsolver2;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Models extends SQLiteOpenHelper {
    private static final String COL_NAME = "name";
    private static final String COL_ID = "id";
    private static final String COL_CONTENT = "content";
    private static final String COL_WORDS = "words";
    private static final String DB_NAME = "crossword_db";
    private static final String TABLE_NAME = "crosswords";
    private static final int DB_VERSION = 1;

    public Models(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format(
                "CREATE TABLE %s (%s TEXT PRIMARY KEY, %s TEXT, %s TEXT)",
                TABLE_NAME, COL_NAME, COL_CONTENT, COL_WORDS
        ));
    }
    public void addCrossword(String name, String content) {
        var db = this.getWritableDatabase();
        var values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_CONTENT, content);
        values.put(COL_WORDS, "");
        db.insert(TABLE_NAME, null, values);
        db.close();
    }
    public void delCrossword(String name) {
        var db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "name=?", new String[] {name});
        db.close();
    }
    public void updateModel(CrosswordInternalModel model) {
        var db = this.getWritableDatabase();
        db.update(TABLE_NAME, model.value(), "name=?", new String[] {model.name});
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL( String.format("DROP TABLE IF EXISTS %s", TABLE_NAME) );
        onCreate(db);
    }
    public List<CrosswordInternalModel> getCrosswords() {
        var list = new ArrayList<CrosswordInternalModel>();
        var db = this.getWritableDatabase();
        var c = db.rawQuery("SELECT * FROM "+TABLE_NAME, null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                list.add(new CrosswordInternalModel(
                        c.getString(0),
                        c.getString(1),
                        c.getString(2)
                ));
                c.moveToNext();
            }
        }
        db.close();
        return list;
    }
    public CrosswordInternalModel getCrossword(String name) {
        var db = this.getWritableDatabase();
        var c = db.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE name=\""+name+"\"", null);
        CrosswordInternalModel item = null;
        if (c.moveToFirst())  {
            item = new CrosswordInternalModel(
                    c.getString(0), c.getString(1), c.getString(2)
            );
        }
        db.close();
        return item;
    }
    public CrosswordInternalModel getEmptyCrossword() {
        var crossword = new CrosswordInternalModel("", "");
        addCrossword(crossword.name, crossword.content);
        return crossword;
    }

    public static class CrosswordInternalModel {
        private String name;
        private String content;
        private Set<String> words;

        public CrosswordInternalModel(String name, String content) {
            this.name = name; this.content = content; this.words = new HashSet<>();
        }
        public CrosswordInternalModel(String name, String content, String words) {
            this.name = name; this.content = content;
            this.words = new HashSet<String>(Arrays.asList(words.split(",")));
        }

        public String getName() { return name; }
        public String getContent() { return content; }
        @SuppressLint("NewApi")
        public ArrayList<String> getWords() { return new ArrayList<>(words); }

        public void setName(String name) { this.name = name; }
        public void setContent(String content) {
            this.content = content;
        }
        public void setWords(List<String> words) {
            this.words = new HashSet<>(words);
        }
        public void addWord(String word) { this.words.add(word); }
        public void removeWord(String word) { this.words.remove(word); }
        public ContentValues value() {
            var values = new ContentValues();
            values.put(COL_NAME, name);
            values.put(COL_CONTENT, content);
            values.put(COL_WORDS, String.join(",", words));
            return values;
        }
    }
}
