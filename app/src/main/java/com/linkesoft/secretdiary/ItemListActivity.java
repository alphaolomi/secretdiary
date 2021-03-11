package com.linkesoft.secretdiary;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.linkesoft.secretdiary.data.Diary;
import com.linkesoft.secretdiary.data.DiaryEntry;
import com.linkesoft.secretdiary.databinding.ActivityItemListBinding;
import com.linkesoft.secretdiary.databinding.ItemListContentBinding;

import java.util.List;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ItemListActivity extends AppCompatActivity implements ILockableActivity {
    private ActivityItemListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitle(getTitle());

        binding.fab.setOnClickListener(view -> {
            DiaryEntry item = Diary.newEntry();
            Context context = view.getContext();
            Intent intent = new Intent(context, ItemDetailActivity.class);
            intent.putExtra(ItemDetailFragment.ARG_ITEM_ID, item.key());

            context.startActivity(intent);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Diary.refresh();
        RecyclerView recyclerView = findViewById(R.id.item_list);
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(Diary.entries()));
    }

    @Override
    public void lock() {
        binding.lock.setVisibility(View.VISIBLE);
    }

    @Override
    public void unlock() {
        binding.lock.setVisibility(View.GONE);
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<DiaryEntry> mValues;

        private final View.OnClickListener mOnClickListener = view -> {
            DiaryEntry item = (DiaryEntry) view.getTag();

            Context context = view.getContext();
            Intent intent = new Intent(context, ItemDetailActivity.class);
            intent.putExtra(ItemDetailFragment.ARG_ITEM_ID, item.key());

            context.startActivity(intent);
        };

        SimpleItemRecyclerViewAdapter(List<DiaryEntry> items) {
            mValues = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            DiaryEntry entry = mValues.get(position);
            holder.titleView.setText(entry.title());
            holder.moodView.setText(entry.getMoodEmoji());

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ItemListContentBinding binding;
            final TextView titleView;
            final TextView moodView;

            ViewHolder(View view) {
                super(view);
                binding = ItemListContentBinding.bind(view);
                titleView = binding.title;
                moodView = binding.mood;
            }
        }
    }
}