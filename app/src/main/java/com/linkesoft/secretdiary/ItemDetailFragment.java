package com.linkesoft.secretdiary;

import android.app.Activity;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.linkesoft.secretdiary.data.Diary;
import com.linkesoft.secretdiary.data.DiaryEntry;
import com.linkesoft.secretdiary.databinding.ItemDetailBinding;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is contained in a {@link ItemDetailActivity}
 * on handsets.
 */
public class ItemDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    private ItemDetailBinding binding;
    DiaryEntry diaryEntry;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            diaryEntry = Diary.entryMap.get(getArguments().getString(ARG_ITEM_ID));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(diaryEntry.title());
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = ItemDetailBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();
        if (diaryEntry != null) {
            try {
                String text = diaryEntry.getText();
                binding.text.setText(text);
            } catch (UserNotAuthenticatedException e) {
                // TODO
            }

        }
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (diaryEntry != null) {
            String text = getText();
            diaryEntry.setText(text);
        }
    }

    public String getText() {
        return binding.text.getText().toString();
    }
}