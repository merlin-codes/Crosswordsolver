package dev.levia.crosswordsolver2;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;

public class Listeners {
    static class OnChangeListener implements TextWatcher {
        private ArrayAdapter<String> adapter;
        @Override public void afterTextChanged(Editable s) { }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        public void setAdapter(ArrayAdapter<String> adapter)  {this.adapter = adapter;}

        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            Log.i("EMPTY", String.format("text changed %s\n", s.toString()));
            adapter.getFilter().filter(s);
        }
    }
}
