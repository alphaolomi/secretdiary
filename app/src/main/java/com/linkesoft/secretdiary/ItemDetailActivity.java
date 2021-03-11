package com.linkesoft.secretdiary;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.linkesoft.secretdiary.data.Diary;
import com.linkesoft.secretdiary.data.DiaryEntry;
import com.linkesoft.secretdiary.databinding.ActivityItemDetailBinding;

import java.io.File;
import java.io.FileWriter;

/**
 * An activity representing a single Item detail screen.
 */
public class ItemDetailActivity extends AppCompatActivity implements ILockableActivity {
    private ActivityItemDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.detailToolbar);

        binding.fab.setOnClickListener(view -> {
            currentEntry().toggleMood();
            binding.fab.setText(currentEntry().getMoodEmoji());//setImageBitmap(currentEntry().moodBitmap());
        });

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don"t need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ItemDetailFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
            ItemDetailFragment fragment = new ItemDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public void lock() {
        binding.lock.setVisibility(View.VISIBLE);
    }

    @Override
    public void unlock() {
        binding.lock.setVisibility(View.GONE);
    }

    private ItemDetailFragment fragment() {
        ItemDetailFragment fragment = (ItemDetailFragment) getSupportFragmentManager().findFragmentById(R.id.item_detail_container);
        return fragment;
    }

    private DiaryEntry currentEntry() {
        return fragment().diaryEntry;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //binding.fab.setImageBitmap(currentEntry().moodBitmap());
        binding.fab.setText(currentEntry().getMoodEmoji());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.item_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, ItemListActivity.class));
            return true;
        } else if (id == R.id.delete) {
            AlertDialog confirmation = new AlertDialog.Builder(this).create();
            confirmation.setTitle(R.string.delete);
            confirmation.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.ok), (dialog, which) -> delete());
            confirmation.show();
        } else if (id == R.id.exportSignature) {
            exportSignature();
        } else if (id == R.id.exportPlainText) {
            exportPlainText();
        } else if (id == R.id.exportPublicKey) {
            exportPublicKey();
        }
        return super.onOptionsItemSelected(item);
    }

    private void delete() {
        currentEntry().delete();
        fragment().diaryEntry = null;
        finish();
    }

    private void exportSignature() {
        String text = fragment().getText();
        File file = currentEntry().getSignatureFile(text);
        startActivity(Intent.createChooser(intentToSend(file), getString(R.string.exportSignature)));
    }

    private void exportPublicKey() {
        File file = Diary.getPublicKeyFile();
        startActivity(Intent.createChooser(intentToSend(file), getString(R.string.exportPublicKey)));
    }

    private void exportPlainText() {
        String text = fragment().getText();
        File file = new File(App.appContext().getCacheDir(), currentEntry().fileName());
        file.delete();
        try {
            FileWriter out = new FileWriter(file);
            out.write(text);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        file.deleteOnExit();
        startActivity(Intent.createChooser(intentToSend(file), getString(R.string.exportPlainText)));
    }

    private Intent intentToSend(File file) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, file.getName());
        intent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri contentUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        } else {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        }
        return intent;
    }
}