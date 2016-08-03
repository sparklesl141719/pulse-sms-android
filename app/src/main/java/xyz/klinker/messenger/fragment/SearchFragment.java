/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import xyz.klinker.messenger.adapter.SearchAdapter;
import xyz.klinker.messenger.data.DataSource;

/**
 * A fragment for searching through conversations and messages.
 */
public class SearchFragment extends Fragment {

    private RecyclerView list;
    private SearchAdapter adapter;
    private String query;
    private DataSource source;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        source = DataSource.getInstance(getActivity());
        source.open();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        list = new RecyclerView(getActivity());
        list.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (adapter == null) {
            adapter = new SearchAdapter(null, null);
        }

        list.setAdapter(adapter);

        return list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (source.isOpen()) {
            source.close();
        }
    }

    public void search(String query) {
        this.query = query;
        loadSearch();
    }

    public boolean isSearching() {
        return query != null && query.length() != 0;
    }

    private void loadSearch() {
        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (source == null) {
                    source = DataSource.getInstance(getActivity());
                }

                if (!source.isOpen()) {
                    source.open();
                }

                final Cursor conversations = source.searchConversations(query);
                final Cursor messages = source.searchMessages(query);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setSearchResults(conversations, messages);
                    }
                });
            }
        }).start();
    }

    private void setSearchResults(Cursor conversations, Cursor messages) {
        if (adapter != null) {
            adapter.updateCursors(conversations, messages);
        } else {
            adapter = new SearchAdapter(conversations, messages);
        }
    }

}
