package jp.sblo.pandora.jota;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.universalchardet.UniversalDetector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;



public class Search
{
    private ArrayList<Record>	mData ;
    private SearchTask mTask;
    private String mQuery;
    private Pattern mPattern;
    private Activity mParent;
    private CharSequence mText;
    OnSearchFinishedListener mListener;

    public static class Record {
        int start;
        int end;
    };
    public interface OnSearchFinishedListener {
        void onSearchFinished( ArrayList<Record> data );
    }

    public Search(Activity parent ,String query , CharSequence text , boolean regexp , boolean ignoreCase , OnSearchFinishedListener listener)
    {
        mData = new ArrayList<Record>();
        mQuery = query;
        mParent = parent;
        mText = text;
        mListener = listener;
        if ( query!=null && query.length() >0 ){
            String patternText = query;
            if ( !regexp ){
                patternText = escapeMetaChar(patternText);
            }

            if ( ignoreCase ){
                mPattern = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE|Pattern.MULTILINE );
            }else{
                mPattern = Pattern.compile(patternText);
            }

            mTask = new SearchTask();
            mTask.execute();
        }
    }

    static public String escapeMetaChar( String pattern )
    {
        final String metachar = ".^$[]*+?|()\\";

        StringBuilder newpat = new StringBuilder();

        int len = pattern.length();

        for( int i=0;i<len;i++ ){
            char c = pattern.charAt(i);
            if ( metachar.indexOf(c) >=0 ){
                newpat.append('\\');
            }
            newpat.append(c);
        }
        return newpat.toString();
    }


    class SearchTask extends AsyncTask<String, Record, Boolean>
    {
        private ProgressDialog mProgressDialog;
        private int mFileCount=0;
        private int mFoundcount=0;
        private boolean mCancelled;

        @Override
        protected void onPreExecute() {
            mCancelled=false;
            mProgressDialog = new ProgressDialog(mParent);
            mProgressDialog.setTitle(R.string.spinner_message);
            mProgressDialog.setMessage(mQuery);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                public void onCancel(DialogInterface dialog)
                {
                    mCancelled=true;
                    cancel(false);
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params)
        {
            if ( isCancelled() ){
                return true;
            }
            Pattern pattern = mPattern;
            Matcher m = pattern.matcher( mText );
            while ( m.find() ){
                Record record = new Record();
                record.start = m.start();
                record.end = m.end();
                mData.add( record );
                if ( mCancelled ){
                    break;
                }
            }
            return true;
        }


        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
            mListener.onSearchFinished(mData);
            mData = null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            onPostExecute(false);
        }


    }

}
