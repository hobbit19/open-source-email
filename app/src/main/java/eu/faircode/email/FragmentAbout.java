package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018 by Marcel Bokhorst (M66B)
*/

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.mail.Address;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FragmentAbout extends FragmentEx {
    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setSubtitle(R.string.menu_about);

        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView tvVersion = view.findViewById(R.id.tvVersion);
        final Button btnDebugInfo = view.findViewById(R.id.btnDebugInfo);

        tvVersion.setText(getString(R.string.title_version, BuildConfig.VERSION_NAME));

        btnDebugInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnDebugInfo.setEnabled(false);
                new SimpleTask<Long>() {
                    @Override
                    protected Long onLoad(Context context, Bundle args) throws UnsupportedEncodingException {
                        StringBuilder sb = new StringBuilder();

                        sb.append(context.getString(R.string.title_debug_info_remark) + "\n\n\n\n");

                        // Get version info
                        sb.append(String.format("%s: %s\r\n", context.getString(R.string.app_name), BuildConfig.VERSION_NAME));
                        sb.append(String.format("Android: %s (SDK %d)\r\n", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
                        sb.append("\r\n");

                        // Get device info
                        sb.append(String.format("Brand: %s\r\n", Build.BRAND));
                        sb.append(String.format("Manufacturer: %s\r\n", Build.MANUFACTURER));
                        sb.append(String.format("Model: %s\r\n", Build.MODEL));
                        sb.append(String.format("Product: %s\r\n", Build.PRODUCT));
                        sb.append(String.format("Device: %s\r\n", Build.DEVICE));
                        sb.append(String.format("Host: %s\r\n", Build.HOST));
                        sb.append(String.format("Display: %s\r\n", Build.DISPLAY));
                        sb.append(String.format("Id: %s\r\n", Build.ID));
                        sb.append("\r\n");

                        sb.append(Helper.getLogcat());

                        String body = "<pre>" + sb.toString().replaceAll("\\r?\\n", "<br />") + "</pre>";

                        EntityMessage draft;

                        DB db = DB.getInstance(context);
                        try {
                            db.beginTransaction();

                            EntityFolder drafts = db.folder().getPrimaryDrafts();
                            if (drafts == null)
                                throw new IllegalArgumentException(context.getString(R.string.title_no_drafts));

                            draft = new EntityMessage();
                            draft.account = drafts.account;
                            draft.folder = drafts.id;
                            draft.msgid = EntityMessage.generateMessageId();
                            draft.to = new Address[]{Helper.myAddress()};
                            draft.subject = context.getString(R.string.app_name) + " debug info";
                            draft.received = new Date().getTime();
                            draft.seen = false;
                            draft.ui_seen = false;
                            draft.ui_hide = false;
                            draft.id = db.message().insertMessage(draft);
                            draft.write(context, body);

                            EntityOperation.queue(db, draft, EntityOperation.ADD);

                            db.setTransactionSuccessful();
                        } catch (IOException ex) {
                            Log.e(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                            return null;
                        } finally {
                            db.endTransaction();
                        }

                        EntityOperation.process(context);

                        return draft.id;
                    }

                    @Override
                    protected void onLoaded(Bundle args, Long id) {
                        btnDebugInfo.setEnabled(true);
                        if (id != null)
                            startActivity(new Intent(getContext(), ActivityCompose.class)
                                    .putExtra("action", "edit")
                                    .putExtra("id", id));
                    }

                    @Override
                    protected void onException(Bundle args, Throwable ex) {
                        btnDebugInfo.setEnabled(true);
                        Toast.makeText(getContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    }
                }.load(FragmentAbout.this, new Bundle());
            }
        });

        return view;
    }
}
