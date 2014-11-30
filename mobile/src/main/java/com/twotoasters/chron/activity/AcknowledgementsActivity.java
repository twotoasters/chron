package com.twotoasters.chron.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.webkit.WebView;

import com.twotoasters.chron.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AcknowledgementsActivity extends ActionBarActivity {

    private static final String ACKNOWLEDGEMENTS_LINK = "file:///android_asset/acknowledgements.html";

    @InjectView(R.id.webView) WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acknowledgements);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.inject(this);
        webView.loadUrl(ACKNOWLEDGEMENTS_LINK);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
