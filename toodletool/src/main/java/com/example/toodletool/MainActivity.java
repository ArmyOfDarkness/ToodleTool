package com.example.toodletool;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.*;
import org.scribe.oauth.OAuthService;

import java.sql.SQLException;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, LoginFragment.ViewReadyListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private FragmentManager manager;
    private LoginFragment loginFragment;
    private TaskFragment taskFragment;
    private Fragment currentFrag;

    private final static String TAG = "AuthTest"; //remove after testing
    private final static String APIKEY = "ToodleTool";
    private final static String APISECRET = "api52e73baa78288";
    private final static String CALLBACK = "www.fake.com";
    private final static String STORED_ACCESS_TOKEN = "accessToken";
    private final static String STORED_ACCESS_SECRET = "accessSecret";
    private final static String STORED_ACCESS_RAWRESPONSE = "accessRawResponse";
    private final static String STORED_TOKEN_TIME = "accessTime";
    private final static String STORED_REFRESH_SECRET = "refreshSecret";
    private static final String USER_ID = "userid";
    private static final String ALIAS = "alias";
    private static final String EMAIL = "email";
    private static final String KEY_TITLE = "title";
    private static final String KEY_STARTDATE = "startDate";
    private static final String KEY_DUEDATE = "dueDate";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_STAR = "star";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_STATUS = "status";

    private Context context;

    private String[] options;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ListView drawerListView;

    private OAuthService service;
    private Token accessToken;
    private WebView webView;
    private TextView userName;

    private SharedPreferences toodleToolPrefs;

    private static SharedPreferences.Editor editor;

    private JSONObject userData;
    private String userId;
    private String alias;
    private String email;

    private ArrayList<JSONObject> tasks;

    //private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        loginFragment = new LoginFragment();
        taskFragment = new TaskFragment();

        tasks = new ArrayList<JSONObject>();

        manager = getSupportFragmentManager();

        manager.beginTransaction().add(R.id.container, loginFragment)
            .add(R.id.container, taskFragment).hide(taskFragment).commit();
        currentFrag = loginFragment;

        mNavigationDrawerFragment = (NavigationDrawerFragment) manager.findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getActionBar().setTitle(title);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, drawerLayout);
        options = getResources().getStringArray(R.array.drawerOptions);
        drawerListView = (ListView) findViewById(R.id.drawerList);
        drawerListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options));
        drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        if ("".equals(toodleToolPrefs.getString(USER_ID, ""))) {
                            break;
                        }
                        //GetTasks getTasks = new GetTasks();
                        //getTasks.execute();
                        manager.beginTransaction().hide(currentFrag).show(taskFragment).commit();
                        currentFrag = taskFragment;
                        drawerLayout.closeDrawer(drawerLayout.getChildAt(1));
                        break;
                    case 1:
                        editor.putString(USER_ID, "");
                        editor.putString(STORED_ACCESS_TOKEN, "");
                        editor.putString(STORED_REFRESH_SECRET, "");
                        editor.commit();
                        drawerLayout.closeDrawer(drawerLayout.getChildAt(1));
                        tasks.clear();
                        AccessTokenAsyncTask task = new AccessTokenAsyncTask();
                        task.execute();
                        break;
                    default:
                        break;
                }
            }
        });

        //remove debug for production
        service = new ServiceBuilder().provider(ToodleApi.class)
                .apiKey(APIKEY).apiSecret(APISECRET).callback(CALLBACK).signatureType(SignatureType.QueryString)
                .build();

        toodleToolPrefs = getSharedPreferences("com.example.toodletool", Context.MODE_PRIVATE);
        editor = toodleToolPrefs.edit();
        Log.d(TAG, "initial prefs test: " + toodleToolPrefs.getString(STORED_ACCESS_TOKEN, "none") + ": " + toodleToolPrefs.getString(STORED_REFRESH_SECRET, "none"));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

    }

    @Override
    public void onViewReadyListener() {
        webView = (WebView) loginFragment.getView().findViewById(R.id.webView);
        userName = (TextView) loginFragment.getView().findViewById(R.id.userName);

        AccessTokenAsyncTask tokenTask = new AccessTokenAsyncTask();
        tokenTask.execute();
    }

    private class AccessTokenAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String storedAccessToken = toodleToolPrefs.getString(STORED_ACCESS_TOKEN, "");
            long storedAccessTokenTime = toodleToolPrefs.getLong(STORED_TOKEN_TIME, -1);
            Log.d(TAG, "Token Time = " + storedAccessTokenTime);
            String storedRefreshSecret = toodleToolPrefs.getString(STORED_REFRESH_SECRET, "");

            if (!"".equals(storedAccessToken) && (System.currentTimeMillis() - storedAccessTokenTime) < 7200000L) {
                Log.d(TAG, "Using stored access Token");
                RefreshTokenHolder.getInstance().setRefreshStatus(false);
                RefreshTokenHolder.getInstance().setAccessStatus(true);
                String token = toodleToolPrefs.getString(STORED_ACCESS_TOKEN, "");
                String secret = toodleToolPrefs.getString(STORED_ACCESS_SECRET, "");
                String response = toodleToolPrefs.getString(STORED_ACCESS_RAWRESPONSE, "");
                accessToken = new Token(token, secret, response);
                Log.d(TAG, "recreated stored access token: " + accessToken.toString());
                return token;
            } else if (!"".equals(storedRefreshSecret) && (System.currentTimeMillis() - storedAccessTokenTime) < 2592000000L) {
                Log.d(TAG, "Using stored refresh Token");

                RefreshTokenHolder.getInstance().setAccessStatus(false);
                RefreshTokenHolder.getInstance().setRefreshStatus(true);
                return toodleToolPrefs.getString(STORED_REFRESH_SECRET, "");
            } else {
                Log.d(TAG, "No good stored Token");
                RefreshTokenHolder.getInstance().setAccessStatus(false);
                RefreshTokenHolder.getInstance().setRefreshStatus(false);
                return "";
            }
        }

        @Override
        protected void onPostExecute(String secret) {
            super.onPostExecute(secret);
            if ("".equals(secret)) {
                try {
                    final String authURL = service.getAuthorizationUrl(Token.empty());
                    manager.beginTransaction().hide(currentFrag).show(loginFragment).commit();
                    currentFrag = loginFragment;
                    webView.setVisibility(View.VISIBLE);
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String Url) {
                            super.shouldOverrideUrlLoading(view, Url);
                            if (Url.contains("www.fake.com")) {
                                webView.setVisibility(View.GONE);
                                Uri uri = Uri.parse(Url);
                                Log.d(TAG, "uri = " + uri);
                                String code = uri.getQueryParameter("code");
                                Log.d(TAG, "code = " + code);
                                final Verifier v = new Verifier(code);
                                Thread t = new Thread() {
                                    public void run() {
                                        accessToken = service.getAccessToken(Token.empty(), v);
                                        Log.d(TAG, "Got raw response: " + accessToken.getRawResponse());
                                        Log.d(TAG, "Got token: " + accessToken.getToken());
                                        Log.d(TAG, "Got secret: " + accessToken.getSecret());
                                        Log.d(TAG, "Got refresh: " + RefreshTokenHolder.getInstance().getSecret());
                                        editor.putString(STORED_ACCESS_TOKEN, accessToken.getToken());
                                        editor.putString(STORED_ACCESS_SECRET, accessToken.getSecret());
                                        editor.putString(STORED_ACCESS_RAWRESPONSE, accessToken.getRawResponse());
                                        editor.putLong(STORED_TOKEN_TIME, System.currentTimeMillis());
                                        editor.putString(STORED_REFRESH_SECRET, RefreshTokenHolder.getInstance().getSecret());
                                        editor.commit();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                GetAccountInfo info = new GetAccountInfo();
                                                info.execute();
                                            }
                                        });
                                    }
                                };
                                t.start();
                            }
                            return true;
                        }
                    });
                    webView.loadUrl(authURL);
                } catch (OAuthException e) {
                    e.printStackTrace();
                }
            } else if (RefreshTokenHolder.getInstance().getRefreshStatus()) {
                try {
                    Thread t = new Thread() {
                        public void run() {
                            Verifier v = new Verifier(toodleToolPrefs.getString(STORED_REFRESH_SECRET, ""));
                            accessToken = service.getAccessToken(Token.empty(), v);
                            Log.d(TAG, "Got secret" + accessToken.getSecret());
                            editor.putString(STORED_ACCESS_TOKEN, accessToken.getToken());
                            editor.putString(STORED_ACCESS_SECRET, accessToken.getSecret());
                            editor.putString(STORED_ACCESS_RAWRESPONSE, accessToken.getRawResponse());
                            editor.putLong(STORED_TOKEN_TIME, System.currentTimeMillis());
                            editor.putString(STORED_REFRESH_SECRET, RefreshTokenHolder.getInstance().getSecret());
                            editor.commit();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    GetAccountInfo info = new GetAccountInfo();
                                    info.execute();
                                }
                            });
                        }
                    };
                    t.start();
                } catch (OAuthException e) {
                    e.printStackTrace();
                }
            } else {
                GetAccountInfo info = new GetAccountInfo();
                info.execute();
            }
        }
    }

    private class GetAccountInfo extends AsyncTask<Void, Void, Response> {
        private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setTitle("Wait");
            progressDialog.setMessage("Getting User Info");
            progressDialog.show();
        }

        @Override
        protected Response doInBackground(Void... params) {
            Response response = null;
            try {
                OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.toodledo.com/3/account/get.php");
                service.signRequest(accessToken, request);
                response = request.send();
                Log.d(TAG, response.getBody());
            } catch (OAuthException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(Response response) {
            try {
                userData = new JSONObject(response.getBody());
                userId = userData.getString(USER_ID);
                alias = userData.getString(ALIAS);
                email = userData.getString(EMAIL);
                editor.putString(USER_ID, userData.getString(USER_ID)).commit();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            progressDialog.dismiss();
            webView.setVisibility(View.GONE);
            userName.setText("Username: " + alias);
        }
    }

    private class GetTasks extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setTitle("Wait");
            progressDialog.setMessage("Getting Tasks");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Response response = null;
            try {
                OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.toodledo.com/3/tasks/get.php");
                service.signRequest(accessToken, request);
                response = request.send();
            } catch(OAuthException e) {
                e.printStackTrace();
            }
            try {
                Log.d(TAG, "get tasks test: " + response.getBody());
                JSONArray array = new JSONArray(response.getBody());
                JSONObject first = array.getJSONObject(0);
                int numTasks = first.getInt("num");
                tasks.clear();
                for (int i = 1; i <= numTasks; i++) {
                    tasks.add(array.getJSONObject(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            manager.beginTransaction().hide(currentFrag).show(taskFragment).commit();
            currentFrag = taskFragment;
            taskFragment.updateTasks();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class TaskFragment extends Fragment {
        private ListView listView;
        private Cursor cursor;
        private TaskDBAdapter dbAdapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            setHasOptionsMenu(true);
            super.onCreate(savedInstanceState);
            dbAdapter = new TaskDBAdapter(context);
            try {
                dbAdapter.open();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_tasks, container, false);
            cursor = dbAdapter.fetchTasks();
            listView = (ListView) view.findViewById(R.id.listView);
            listView.setAdapter(new TaskAdapter(getActivity(), cursor, 0));
            return view;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.task, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.newTask:
                    NewTaskFragment newTask = new NewTaskFragment(context, service, accessToken);
                    manager.beginTransaction().hide(currentFrag).add(R.id.container, newTask, "NewTask").commit();
                    currentFrag = newTask;
                    return true;
                case R.id.sync:
                    Cursor newTaskCursor = dbAdapter.fetchNewTasks();
                    if (cursor.getCount() > 0) {
                        SyncTasks sync = new SyncTasks();
                        sync.execute(newTaskCursor);
                    }
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        public void updateTasks() {
            ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
        }

        private class SyncTasks extends AsyncTask<Cursor, Void, Response> {
            private ProgressDialog progressDialog = new ProgressDialog(context);

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setTitle("Wait");
                progressDialog.setMessage("Submitting Tasks");
                progressDialog.show();
            }

            @Override
            protected Response doInBackground(Cursor... params) {
                OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.toodledo.com/3/tasks/add.php");
                service.signRequest(accessToken, request);
                Cursor cursor = params[0];
                cursor.moveToFirst();
                //get column numbers
                final int TITLE_INDEX = cursor.getColumnIndex(KEY_TITLE);
                final int STAR_INDEX = cursor.getColumnIndex(KEY_STAR);
                final int START_INDEX = cursor.getColumnIndex(KEY_STARTDATE);
                final int DUE_INDEX = cursor.getColumnIndex(KEY_DUEDATE);
                final int PRIORITY_INDEX = cursor.getColumnIndex(KEY_PRIORITY);
                final int STATUS_INDEX = cursor.getColumnIndex(KEY_STATUS);
                final int NOTES_INDEX = cursor.getColumnIndex(KEY_NOTES);
                String json = "[{";
                while (!cursor.isAfterLast()) {

                    String taskTitle = cursor.getString(TITLE_INDEX);
                    int star = cursor.getInt(STAR_INDEX);

                    if (!cursor.isFirst()) {
                        json += ",{";
                    }
                    json += "\"title\":\"" + taskTitle + "\",\"star\":" + star;
                    if (!cursor.isNull(START_INDEX)) {
                        json += ",\"startdate\":" + cursor.getLong(START_INDEX);
                    }
                    if (!cursor.isNull(DUE_INDEX)) {
                        json += ",\"duedate\":" + cursor.getLong(DUE_INDEX);
                    }
                    if (!cursor.isNull(PRIORITY_INDEX)) {
                        json += ",\"priority\":" + cursor.getInt(PRIORITY_INDEX);
                    }
                    if (!cursor.isNull(STATUS_INDEX)) {
                        json += ",\"status\":" + cursor.getInt(STATUS_INDEX);
                    }
                    if (!cursor.isNull(NOTES_INDEX)) {
                        json += ",\"notes\":\"" + cursor.getString(NOTES_INDEX) + "\"";
                    }
                    json += "}";
                    cursor.moveToNext();
                }
                json += "]";
                request.addBodyParameter("tasks", json);
                //Log.d("Sync test: ", json);
                Response response = request.send();
                return response;
            }

            @Override
            protected void onPostExecute(Response response) {
                progressDialog.dismiss();
                if (response.getBody().contains("error")) {
                    Log.d("Sync test: ", response.toString());
                    Toast.makeText(context, "Error submitting task", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Submitted successfully", Toast.LENGTH_SHORT).show();
                }
                Log.d("Sync test: ", response.getBody());
            }
        }
    }
}
